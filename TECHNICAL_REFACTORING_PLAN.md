# Technical Refactoring Plan

**Scope:** Architecture, code-quality, and structural refactoring for the Equilibrium Android puzzle game. Derived directly from the findings in `PROJECT_AUDIT_REPORT.md`.
**Audience:** Human engineers and AI coding agents. Each item is self-contained, sequenced, and verifiable.
**Hard constraint (from product owner):** Do **not** remove, disable, or relocate advertisement systems. All existing **test ad IDs are preserved**; production IDs are introduced *alongside* them via build-variant gating (see `CRIT-2`).

---

## 0. Current Problems (Architectural)

| ID | Issue | Why it's problematic | Impact | Risk |
|---|---|---|---|---|
| A1 | `GameViewModel` is a **god object** (~540 LOC, ~20 `MutableStateFlow`s, game rules + timer + persistence + audio + ad glue). | Mixing concerns means any rule change touches everything; impossible to unit-test in isolation. | Maintainability + testability collapse. | High |
| A2 | The core **±1 tap mechanic is duplicated in 4 places**: `LevelGenerator.reverseTap`, `GameSolver.applyTap`, `GameViewModel.tapCell`, and tutorial text. | Behaviour can drift between copies; bugs fixed in one are not fixed in others. | Correctness risk, the heart of the game. | High |
| A3 | `GameScreen` is a **god composable (~1000 LOC)** with win/pause/time-up/tutorial dialogs **nested inside `if (winState)`** (`GameScreen.kt:474+`). | In-game Settings and the Master "Time's Up" dialog are structurally unreachable mid-game. | Live UX bug + huge file untestable. | High |
| A4 | UI **directly couples to `AdManager`** (casts `context as? Activity`, calls `AdManager.showRewarded/showInterstitial`). | Untestable UI; cannot mock ad layer; violates MVVM. | Test + swap difficulty. | Medium |
| A5 | `GameViewModel.tapCell(..., context: Context)` leaks a platform dep just to get the `Vibrator`. | ViewModel is no longer plain Kotlin; breaks engine purity. | Testability. | Medium |
| A6 | **No DI container** — `MainActivity` hand-wires DB → Repository → ViewModel. | Duplication (`onCreate` vs `onResume` rebuild the graph); hard to swap for tests. | Maintainability. | Medium |
| A7 | **Single Gradle module** holds UI, engine, data, ad, audio. | No enforcement of layer boundaries; slow incremental builds. | Scalability. | Low |
| A8 | **Fake "online" leaderboard** (`DailyLeaderboardGenerator`) with no network. | Deceptive UX if marketed as multiplayer; dead `DailyLeaderboardDialog`. | Trust / store policy. | Medium |

---

## 1. Proposed Solutions

### A1 → Split `GameViewModel` into cohesive collaborators

**Solution.** Decompose into:
- `GameEngineState` — pure board state, `tapCell` rule application, win check, star calc (no Android).
- `TimerController` — owns `secondsElapsed`, `countdownSecondsLeft`, `isTimeUp`, the `timerJob`. Single source of the timer loop (removes the duplicated block — see `Q7`/`A2-adjacent`).
- `SaveManager` — owns serialize/deserialize + `writeActiveGameSave`/`resumeActiveGame`, with validation.
- `EconomyController` — undo/hint/skip accounting delegating to `GameRepository`.
- `GameViewModel` — thin facade that wires the above to `viewModelScope` + exposes `StateFlow`s for Compose.

**Steps.**
1. Extract `GameEngineState` + its unit tests first (pure — easiest to verify).
2. Move timer logic into `TimerController`; delete the duplicated resume timer block.
3. Move save logic into `SaveManager`; add try/catch validation on deserialize.
4. Re-point `GameViewModel` at the new collaborators.

**Benefits.** Each piece <150 LOC, independently testable, single responsibility.
**Migration.** Keep the public API of `GameViewModel` (the fields `MainActivity` reads) stable so the NavHost doesn't change; refactor internals.

### A2 → Single source of truth for the mechanic

**Solution.** Create `data/engine/GameRules.kt`:

```kotlin
object GameRules {
    private val DR = intArrayOf(-1, 1, 0, 0)
    private val DC = intArrayOf(0, 0, -1, 1)

    /** The player's tap: center -1, neighbours +1. Mutates [board] in place. */
    fun applyTap(board: Array<IntArray>, r: Int, c: Int, size: Int) {
        board[r][c] -= 1
        for (i in 0 until 4) {
            val nr = r + DR[i]; val nc = c + DC[i]
            if (nr in 0 until size && nc in 0 until size) board[nr][nc] += 1
        }
    }

    /** The generator's inverse: center +1, neighbours -1. Mutates in place. */
    fun applyReverseTap(board: Array<IntArray>, r: Int, c: Int, size: Int) {
        board[r][c] += 1
        for (i in 0 until 4) {
            val nr = r + DR[i]; val nc = c + DC[i]
            if (nr in 0 until size && nc in 0 until size) board[nr][nc] -= 1
        }
    }

    fun isSolved(board: Array<IntArray>, size: Int): Boolean { /* all-equal check */ }
    fun equilibriumProgress(board: Array<IntArray>, size: Int): Float { /* single impl */ }
}
```

Replace all 4 duplicated implementations (including `GameViewModel.calculateEquilibriumProgress` and the inline copy in `GameScreen`) with calls to `GameRules`.

**Benefits.** One place to change the rule; behaviour can never drift.
**Migration.** Mechanical replacement; covered by new engine tests (see `TECH-TEST` below).

### A3 → Flatten `GameScreen` dialog tree

**Solution.** Hoist the pause-settings, time-up, and tutorial dialogs **out of** the `if (winState)` block to siblings under the `Scaffold`. Extract `ConfettiOverlay` + each dialog into its own file under `ui/screens/game/`.
**Steps.** (1) Move `showPauseSettings`, `isTimeUp`, `tutorialActive` states to top of composable. (2) Render each dialog as a sibling of the win dialog. (3) Split file into `GameScreen.kt`, `WinDialog.kt`, `PauseSettingsDialog.kt`, `TimeUpDialog.kt`, `TutorialOverlay.kt`, `ConfettiOverlay.kt`.
**Benefits.** Settings reachable mid-game; file sizes sane; each dialog previewable/testable.
**Migration.** Pure structural move; no API change.

### A4 → Introduce an `AdController` interface consumed by the ViewModel

**Solution.**
```kotlin
interface AdController {
    fun showInterstitial(activity: Activity, onClosed: () -> Unit)
    fun showRewarded(activity: Activity, onReward: (Int) -> Unit, onClosed: () -> Unit)
}
```
Implement `AdMobAdController` wrapping the existing `AdManager` (which keeps its IDs/logic unchanged). Inject `AdController` into the ViewModel; Composable calls `vm.onUndoExhausted()` instead of `AdManager.showRewarded(...)`.

**Benefits.** UI no longer casts `context as? Activity` for ads; ad layer is mockable in Compose UI tests; the real ad system is untouched.
**Migration.** Wrap — do not rewrite — `AdManager`. Additive only.

### A5 → `HapticController` abstraction

**Solution.** `interface HapticController { fun tap() }` + `AndroidHapticController(context)`. Inject into ViewModel; remove the `Context` param from `tapCell`.

### A6 → Dependency Injection (Hilt)

**Solution.** Add `dagger-hilt` (or Koin). `@HiltAndroidApp` Application, `@Module` providing `AppDatabase`, DAOs, `GameRepository`, `AdController`, `HapticController`, `Clock`. `GameViewModel` annotated `@HiltViewModel`; `MainActivity` `@AndroidEntryPoint`.

### A7 → Multi-module split

**Solution.** New Gradle modules: `:core-engine` (pure Kotlin), `:core-data` (Room + repo), `:ad` (`AdController` + AdMob impl), `:ui` (Compose), `:app` (Application + DI graph). `:core-engine` has zero Android deps.
**Migration.** Do **after** A1–A6 on a green build; move packages module-by-module.

### A8 → Honest leaderboard

**Solution.** Rename `generateOnlineLeaderboardForDate` → `generateSimulatedLeaderboard`; relabel UI "Practice Rivals — simulated field". Either keep as an offline motivational feature or stub a real backend later. Delete or wire the dead `DailyLeaderboardDialog`.

---

## 2. Phased Refactoring Plan

### Phase 1 — Critical fixes (week 1)
- **CRIT-1** Remove hardcoded developer email (`DailyLeaderboardGenerator.kt:58`).
- **CRIT-2** Build-variant-gated AdMob IDs that **preserve test IDs** for debug (see §3 below).
- **CRIT-3** Validate auto-save deserialization (try/catch + fallback).
- **CRIT-4** Hoist `GameScreen` dialogs out of `if (winState)` (partial A3).

### Phase 2 — Structural improvements (weeks 1–2)
- **STRUCT-1** `GameRules` single source of truth (A2).
- **STRUCT-2** Split `GameViewModel` into engine/timer/save/economy (A1).
- **STRUCT-3** Extract `AdController` + `HapticController` (A4, A5).
- **STRUCT-4** Decompose `GameScreen` into files (rest of A3).
- **STRUCT-5** Hilt DI (A6).

### Phase 3 — Performance improvements (week 3)
- See `PERFORMANCE_AND_STABILITY_PLAN.md`.

### Phase 4 — Feature improvements (weeks 3–4)
- Honest leaderboard relabel, endless-level UI, debounced autosave, settings improvements.

### Phase 5 — Production preparation (week 4)
- See `AI_AGENT_TASK_BACKLOG.md` (production-readiness checklist).

---

## 3. Critical Remedies (detailed, copy-ready)

### CRIT-1 — Remove hardcoded developer email

**File:** `app/src/main/java/com/example/data/engine/DailyLeaderboardGenerator.kt:58`

```diff
- username = "You (tinupadikkalathomas@gmail.com)",
+ username = "You",
```

**Optional (better):** add a `displayName: String = "Player"` column to `UserStats` (requires DB migration — bump `version` to 6 with an `ALTER TABLE` migration), and use `userScore.displayName`. The minimal one-line change above fully resolves the PII issue and is the recommended first step.

**Verify:** `grep -rn "tinupadikkalathomas" app/` returns nothing.

### CRIT-2 — Build-variant-gated AdMob IDs (PRESERVES test IDs)

> Constraint reminder: test IDs must **not** be removed. This scheme keeps Google's official test IDs as the **debug default**, and only the **release** build overrides them — loaded from a gitignored secret. If the secret is missing, the **release build fails** so test IDs can never accidentally ship.

#### Step 1 — `.env.example` (committed, placeholder) and `.env` (gitignored, real)
```env
# .env.example  (committed)
GEMINI_API_KEY=MY_GEMINI_API_KEY
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
BANNER_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
INTERSTITIAL_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
REWARDED_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
```
```env
# .env  (gitignored — your real IDs)
ADMOB_APP_ID=ca-app-pub-<your-pub-id>~<your-app-id>
BANNER_AD_UNIT_ID=ca-app-pub-<your-pub-id>/<your-unit-id>
INTERSTITIAL_AD_UNIT_ID=ca-app-pub-<your-pub-id>/<your-unit-id>
REWARDED_AD_UNIT_ID=ca-app-pub-<your-pub-id>/<your-unit-id>
```

#### Step 2 — `app/build.gradle.kts` (additive — keeps test IDs as defaults)
```kotlin
android {
    defaultConfig {
        // DEFAULTS = Google's official TEST IDs (debug uses these verbatim — preserved).
        buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }
    buildTypes {
        release {
            // REAL IDs only — fail the build if absent so test IDs can never ship.
            fun realId(key: String) = (project.findProperty(key) ?: System.getenv(key)
                ?: error("Release build missing $key — set it in .env")).toString()
            buildConfigField("String", "ADMOB_APP_ID", "\"${realId("ADMOB_APP_ID")}\"")
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${realId("BANNER_AD_UNIT_ID")}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${realId("INTERSTITIAL_AD_UNIT_ID")}\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${realId("REWARDED_AD_UNIT_ID")}\"")
            manifestPlaceholders["ADMOB_APP_ID"] = realId("ADMOB_APP_ID")
        }
    }
}
```

#### Step 3 — `AdManager.kt` (read from BuildConfig; test IDs still compiled in for debug)
```kotlin
import com.example.BuildConfig

object AdManager {
    private const val TAG = "AdManager"

    // Debug build → test IDs (unchanged behaviour). Release build → real IDs.
    val BANNER_AD_UNIT_ID: String = BuildConfig.BANNER_AD_UNIT_ID
    val INTERSTITIAL_AD_UNIT_ID: String = BuildConfig.INTERSTITIAL_AD_UNIT_ID
    val REWARDED_AD_UNIT_ID: String = BuildConfig.REWARDED_AD_UNIT_ID

    // ...loadInterstitial / loadRewarded / AdmobBanner now reference the vals above,
    //    e.g. INTERSTITIAL_TEST_ID  →  INTERSTITIAL_AD_UNIT_ID ...
}
```

#### Step 4 — `AndroidManifest.xml` (placeholder instead of hardcoded test ID)
```diff
- <meta-data
-     android:name="com.google.android.gms.ads.APPLICATION_ID"
-     android:value="ca-app-pub-3940256099942544~3347511713"/>
+ <meta-data
+     android:name="com.google.android.gms.ads.APPLICATION_ID"
+     android:value="${ADMOB_APP_ID}"/>
```

#### Step 5 — Test devices (so real release IDs still serve test creatives to developers)
In `AdManager.initialize`:
```kotlin
val config = RequestConfiguration.Builder()
    .setTestDeviceIds(listOf("TEST_DEVICE_HASH_FROM_LOGCAT"))
    .build()
MobileAds.setRequestConfiguration(config)
```

**Verify (constraint check):**
- Debug build: ad requests still hit `ca-app-pub-3940256099942544/*` (test) → ads preserved.
- Release build without `.env`: gradle `error(...)` aborts → no test-ID leak.
- Release build with `.env`: real IDs injected; placements (banner/interstitial/rewarded) unchanged in count and location.
- `grep -rn "ca-app-pub-3940256099942544" app/src` now only matches the debug `defaultConfig` + `.env.example` (intentional).

### CRIT-3 — Validate auto-save deserialization

**File:** `app/src/main/java/com/example/ui/viewmodel/GameViewModel.kt` (`deserializeBoard`, `deserializeHistory`)

```kotlin
private fun deserializeBoard(str: String): Array<IntArray>? = try {
    val rows = str.split(";")
    Array(rows.size) { r ->
        val cols = rows[r].split(",")
        IntArray(cols.size) { c -> cols[c].toInt() }
    }
} catch (e: Exception) {
    null   // caller falls back to a fresh level
}
```
`resumeActiveGame` then: if any deserialization returns null, delete the corrupt save and `loadLevel(1)` instead of crashing.

**Verify:** unit test feeds `"1,2;3,X"` and asserts `null` (no throw).

### CRIT-4 — Hoist GameScreen dialogs

Already described in A3. Minimum to unblock the bug: move `if (showPauseSettings)`, `if (isTimeUp ...)`, `if (tutorialActive)` blocks from inside the `if (winState) { Box { ... } }` to be **siblings** at the top level of the composable (outside the win `Box`).

**Verify:** open a game, tap Settings icon mid-game → dialog appears (previously impossible).

---

## 4. Code Quality Improvement Plan

| Task | Priority | Effort | Dependencies | Expected outcome |
|---|---|---|---|---|
| Remove dead `DailyLeaderboardDialog` or wire it | Medium | 0.5d | — | No unreferenced 190-LOC composable. |
| Delete unused imports (`Brush`, `LevelGenerator`, `LeaderboardEntry`, `ExperimentalFoundationApi` in HomeScreen) | Low | 0.1d | — | Clean compile warnings. |
| Delete unused `SoundEffectPlayer.playSuccess()` or call it on win | Low | 0.1d | — | No dead method. |
| Collapse duplicate `formatDateKey`/`formatTime` into `ui/util/Formats.kt` | Medium | 0.3d | — | One formatter pair. |
| Collapse duplicate timer block (startTimer vs resumeActiveGame) into `TimerController` | High | 0.5d | A1 | Single timer impl. |
| Replace magic numbers (`/12.0f`, `98765`, `delay(4000)`) with named constants | Low | 0.3d | — | Readable intent. |
| Route hardcoded `Color(0xFF…)` through `MaterialTheme.colorScheme` | Medium | 0.5d | — | Themes apply everywhere. |
| Remove unused deps (Retrofit/OkHttp/Moshi/Firebase BOM) or wire them | High | 0.3d | — | Smaller APK; no dead surface. |
| Bump Compose BOM (2024.09.00 → current) | Medium | 0.3d | — | Bug fixes; deprecations cleared. |
| Adopt Material typography scale instead of ad-hoc `sp` | Low | 0.5d | — | Consistent type ramp. |
| KDoc on all `public` engine + repo APIs | Medium | 0.5d | A1, A2 | Onboarding doc. |
| Naming: rename `GameSolver` → `HintEngine` (it's a heuristic, not a solver) | Low | 0.2d | A2 | Honesty in names. |

---

## 5. Execution order (dependency-aware)

```
CRIT-1, CRIT-2, CRIT-3, CRIT-4   (parallel, week 1)
        │
        ▼
STRUCT-1 (GameRules) ──► STRUCT-2 (split VM) ──► STRUCT-3 (AdController/Haptic)
                                                     │
                                                     ▼
                                               STRUCT-5 (Hilt) ──► STRUCT-4 (split GameScreen)
                                                                                       │
                                                                                       ▼
                                                                               A7 multi-module
```

Every task above is mirrored as a ticket in `AI_AGENT_TASK_BACKLOG.md`.
