# Comprehensive Software Project Audit & Technical Assessment

**Project:** Equilibrium — *"One tap. Infinite ripples. Find the balance in this addictive mathematical puzzle game."*
**Audit Date:** 2026-06-21
**Auditor Method:** Static, file-by-file review of the full repository
**Deliverable:** `PROJECT_AUDIT_REPORT.md`

---

## Executive Summary

Equilibrium is a **single-module, Kotlin/Android mobile puzzle game** built with **Jetpack Compose**. The core mechanic is a *grid-balancing* puzzle: tapping a cell decrements it by 1 and increments its orthogonal neighbours by 1; the goal is to make every cell on the grid equal. It features procedurally generated levels, a daily challenge with a *fake* "online" leaderboard, two difficulty modes (Zen / Master), themes, undo/hint/skip economy, Room-backed persistence, an auto-save system, procedural audio, and Google AdMob monetization (test ads).

The project is **small (≈18 hand-written Kotlin files, ~2.5k LOC)** and was clearly **AI-generated from an AI Studio template** (it ships `general_1.md` / `general_2.md` audit prompts, references an AI Studio app URL, and contains leftover templating). It is functional as a prototype/personal app, but is **not production-ready**: it ships a *hardcoded developer email* in user-facing strings, uses AdMob **test ad-unit IDs** that will earn no revenue and violate Play policy if shipped as-is, has **no real backend** despite a misleading "online leaderboard", and carries **dead code, unused dependencies, and a self-contradictory signing/release config**.

**Overall verdict:** A coherent, reasonably organized MVP/prototype that needs meaningful hardening before any public or commercial release.

### Scores at a Glance

| Category | Score |
|---|---|
| Architecture | 6.5/10 |
| Code Quality | 5.5/10 |
| Stability | 6.0/10 |
| Game Engine | 6.0/10 |
| Performance | 6.5/10 |
| Security | 4.0/10 |
| UI | 6.5/10 |
| UX | 6.0/10 |
| Testing | 2.5/10 |
| Production Readiness | 3.5/10 |

**Crash Risk Assessment: Low–Medium.** The app is largely defensive (null checks, try/catch around audio/haptics). The realistic crash vectors are narrow: config-change/process-death loss of in-memory game state, a corrupted auto-save row causing `NumberFormatException` on deserialization, and the `AudioTrack`/`ToneGenerator` paths on edge-case devices. See Phase 4.

---

## Phase 1 — Project Discovery

### Folder tree summary

```
equilibrium/
├── build.gradle.kts                 # Root Gradle (plugin aliases only)
├── settings.gradle.kts              # Single module (:app), rootProject.name="My Application"
├── gradle.properties                # JVM args, parallel, configuration-cache
├── local.properties                 # sdk.dir=/opt/android/sdk  (committed!)
├── metadata.json                    # AI Studio metadata
├── .env.example                     # GEMINI_API_KEY placeholder
├── README.md                        # AI Studio "Run and deploy" template
├── general_1.md / general_2.md      # The two audit prompts themselves
├── gradle/libs.versions.toml        # Version catalog
└── app/
    ├── build.gradle.kts             # App config + signing + dependencies
    ├── proguard-rules.pro           # Empty (template comments only)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/                  # mipmaps (launcher), values, xml backup rules
        │   └── java/com/example/
        │       ├── MainActivity.kt
        │       ├── ad/AdManager.kt
        │       ├── sound/{SoundEffectPlayer,AmbientMusicPlayer}.kt
        │       ├── data/
        │       │   ├── database/    # AppDatabase + 4 entities + 4 DAOs
        │       │   ├── engine/      # LevelGenerator, GameSolver, DailyLeaderboardGenerator
        │       │   └── repository/GameRepository.kt
        │       └── ui/
        │           ├── components/  # AnimatedTile, AdmobBanner
        │           ├── screens/     # Home, Game, DifficultySelect
        │           ├── theme/       # Color, Theme, Type
        │           └── viewmodel/GameViewModel.kt
        ├── test/                    # Example unit + Robolectric screenshot tests
        └── androidTest/             # Example instrumented test
```

### Identified characteristics

| Dimension | Finding |
|---|---|
| Programming languages | **Kotlin** (JVM target 11). A handful of XML resources/markup. No other languages. |
| Framework | **Jetpack Compose** (Material 3) + AndroidX (lifecycle, navigation, activity). |
| Libraries | Room (DB), Navigation-Compose, Coroutines, OkHttp + Retrofit + Moshi (present but unused), AdMob (play-services-ads), Robolectric/Roborazzi (tests). |
| Game engine | **None — custom hand-rolled logic** in `data/engine`. No Unity/LibGDX. The "engine" is pure functions over `Array<IntArray>`. |
| Build tools | **Gradle (Kotlin DSL, AGP 9.1.1)** + version catalog. KSP for Room/Moshi codegen. |
| Package managers | Gradle (no npm/pip/etc.). |
| Database systems | **Room (SQLite)** — single DB `equilibrium_database`, version 5, 4 tables. |
| External services/APIs | **AdMob** (test IDs). A **Gemini API key** is plumbed (`.env`, secrets plugin, Firebase BOM) but **no AI code actually calls it**. No backend server. |
| Configuration files | `build.gradle.kts`, `libs.versions.toml`, `gradle.properties`, `local.properties`, `.env.example`, `AndroidManifest.xml`, `proguard-rules.pro`. |
| CI/CD files | **None.** No GitHub Actions/GitLab CI/Bitrise/fastlane. |
| Documentation files | `README.md` (boilerplate AI-Studio run instructions only). No architecture doc, no CONTRIBUTING, no LICENSE. |

### Major components

1. **App shell / Navigation** — `MainActivity.kt` (single `ComponentActivity`, `NavHost` with 3 routes: `home`, `difficulty_select`, `game`).
2. **Presentation** — Compose screens + `AnimatedTile` component + 6 Material color themes.
3. **State holder** — `GameViewModel` (one big ViewModel holding ~20 `MutableStateFlow`s and the entire game logic).
4. **Domain/Engine** — `LevelGenerator` (procedural scrambling), `GameSolver` (hint picker), `DailyLeaderboardGenerator` (fake competitor list).
5. **Data layer** — `GameRepository` + Room (`AppDatabase`, 4 DAOs, 4 entities).
6. **Cross-cutting** — `AdManager` (interstitial/rewarded/banner), `SoundEffectPlayer` (DTMF tones), `AmbientMusicPlayer` (procedural `AudioTrack` chord synth).

### Dependency graph (logical)

```
            ┌────────────── MainActivity (host + NavHost) ──────────────┐
            │                                                            │
            ▼                                                            ▼
   GameViewModel ──► GameRepository ──► DAOs ──► Room/SQLite
        │   │
        │   ├──► LevelGenerator / GameSolver / DailyLeaderboardGenerator   (pure engine)
        │   ├──► SoundEffectPlayer / AmbientMusicPlayer                    (audio)
        │   └──► AdManager                                                 (monetization)
        ▲
   Compose Screens (Home / Game / DifficultySelect) ──► AnimatedTile / AdmobBanner ──► AdManager
```

### High-level architecture diagram (Markdown)

```
┌─────────────────────────────── UI LAYER (Compose) ───────────────────────────────┐
│  HomeScreen  │  DifficultySelectScreen  │  GameScreen  │  AnimatedTile/AdmobBanner│
└────────────────────────────────────┬─────────────────────────────────────────────┘
                                     │ state + callbacks
┌────────────────────────────────────▼────────────── (ViewModel) ──────────────────┐
│                            GameViewModel (god object)                             │
│  boardState · movesCount · timer · winState · hints/undo/skip · save/restore      │
└──┬──────────────────┬───────────────────────────┬────────────────────────────────┘
   │ engine (pure)    │ data (Room)               │ platform services
   ▼                  ▼                           ▼
LevelGenerator    GameRepository            AdManager / SoundEffectPlayer /
GameSolver        └─ 4 DAOs → AppDatabase   AmbientMusicPlayer
DailyLeaderboard       (SQLite)
```

---

## Phase 2 — Architecture Analysis

### Architectural style

**Layered + MVVM, single-module (monolithic-by-module), event-driven via Compose state.** Presentation → ViewModel → Repository → DAO/Room, plus a small "engine" package of pure stateless functions. There is no DI container — `MainActivity` performs manual constructor injection of the repository into the ViewModel via a custom `ViewModelProvider.Factory`.

### Separation of concerns

- **Good:** Engine logic (`LevelGenerator`, `GameSolver`) is decoupled from Android and individually testable. Repository cleanly wraps DAOs. Screens are stateless/parametric composables (good for preview/tests).
- **Weak:** `GameViewModel` is a **god object** (~540 LOC) combining game rules, timer orchestration, board serialization, persistence triggers, ad/sound/haptic side-effects, and scoring. `MainActivity` also mixes wiring with business logic (it recomputes `bestTimeSeconds` inline in the NavHost, line ~148–152).

### Coupling

- `GameScreen` is **directly coupled to `AdManager` and `Activity`** (casts `context as? Activity`, calls `AdManager.showRewarded/showInterstitial`). UI talking directly to the ad SDK bypasses the ViewModel — harder to test and to swap.
- `GameViewModel.tapCell(...)` takes an `android.content.Context` purely to fetch the `Vibrator`. That platform dependency leaks into a class that is otherwise engine-like.
- `MainActivity` reads `userStats?.themeName` and decides music on/off — duplicating logic already in `onResume`.

### Cohesion

- High cohesion in: `LevelGenerator`, `GameSolver`, DAOs, `AmbientMusicPlayer`.
- Low cohesion in: `GameViewModel` (timer + serialization + scoring + save + economy + UI state all in one type).

### Scalability / Maintainability / Extensibility / Testability

- **Scalability:** Adequate for an offline single-player puzzle. The architecture would not scale to multiplayer/cloud without significant restructuring (there is no real networking layer — Retrofit/OkHttp are included but unused).
- **Maintainability:** Moderate. The codebase is small and readable, but the god ViewModel and the duplication of date/timer/serialization logic (see Phase 3) hurt it.
- **Extensibility:** Reasonable for content (new levels/themes are easy) but poor for mechanics — adding any new game rule means editing the god ViewModel.
- **Testability:** Engine is testable; **almost nothing else has tests** (see Phase 10). The ViewModel is hard to unit-test because of the `Context` dependency and direct coroutine launches.

### Answers

- **Is the project truly modular?** *Partially.* It uses packages, but everything lives in a single Gradle module, and one ViewModel owns most of the behavior.
- **Which parts violate modular design?** `GameViewModel` (kitchen-sink), `GameScreen` (UI calling ad SDK directly), and `MainActivity` (business logic inline).
- **Which systems are tightly coupled?** UI ↔ AdManager; ViewModel ↔ Android `Context`/`Vibrator`; MainActivity ↔ DB/repository/ViewModel wiring.
- **What should be refactored?** (1) Split `GameViewModel` into `GameEngineState` + `TimerController` + `SaveManager` + `EconomyRepository`. (2) Introduce Hilt/Koin DI. (3) Wrap AdMob behind an `AdController` interface consumed by the ViewModel, not the Composable. (4) Extract a `Clock`/`DateProvider` so the day-of-year streak logic is testable. (5) Split UI into `:app`, `:core-engine`, `:data`, `:ui` Gradle modules.

### Architecture Score: **6.5 / 10**

Solid MVVM bones and clean engine/data packages, dragged down by a god ViewModel, ad/UI coupling, and lack of a real module/DI boundary.

---

## Phase 3 — Code Quality Audit

### Systemic issues

| # | Issue | Where | Severity |
|---|---|---|---|
| Q1 | **God object** `GameViewModel` (~540 LOC, ~20 state fields, game logic + timer + persistence + audio + ads glue) | `ui/viewmodel/GameViewModel.kt` | High |
| Q2 | **God composable** `GameScreen` (~1000 LOC including win/pause/time-up/tutorial dialogs and the entire `ConfettiOverlay` particle system) | `ui/screens/GameScreen.kt` | High |
| Q3 | **Dead UI code**: `DailyLeaderboardDialog` (≈190 LOC) and its `LeaderboardEntry` producer are defined but **never invoked** anywhere. The fake leaderboard generator runs only if the dialog is opened, which it never is. | `HomeScreen.kt:945`, `DailyLeaderboardGenerator.kt` | Medium |
| Q4 | **Unused imports**: `Brush` (`HomeScreen.kt:19`), `LevelGenerator` (`HomeScreen.kt:37`), `LeaderboardEntry` (`HomeScreen.kt:36`). `ExperimentalFoundationApi` annotation import (`HomeScreen.kt:4`) is unused. | `HomeScreen.kt` | Low |
| Q5 | **Unused public method**: `GameViewModel.calculateEquilibriumProgress()` is duplicated inline inside `GameScreen` (which re-derives the same min/max formula). One of them is dead. | `GameViewModel.kt:507`, `GameScreen.kt:81` | Medium |
| Q6 | **Unused function**: `SoundEffectPlayer.playSuccess()` is never called (only `playTap` and `playWin` are). | `sound/SoundEffectPlayer.kt:25` | Low |
| Q7 | **Duplicate logic** — the Master-mode countdown timer block is copy-pasted verbatim between `startTimer()` (lines 62–79) and `resumeActiveGame()` (lines 202–219). Two copies to keep in sync. | `GameViewModel.kt` | High |
| Q8 | **Duplicate formatting helpers** — `formatDateKey` and `formatTime` are copy-pasted identically in both `HomeScreen.kt` (lines 1134–1148) and `GameScreen.kt` (lines 1007–1021). | screens | Medium |
| Q9 | **Commented-out dependency bloat** — 11 of the catalog entries are commented out in `app/build.gradle.kts` (camera×4, accompanist, datastore, coil, material-icons-extended, play-services-location, firebase-ai). Catalog also keeps `firebase-bom` as an *active* `implementation` even though `firebase-ai` is commented and no Firebase code exists. | `app/build.gradle.kts`, `libs.versions.toml` | Medium |
| Q10 | **Hardcoded developer identity** in a user-facing string: `"You (tinupadikkalathomas@gmail.com)"` is baked into the fake leaderboard. | `DailyLeaderboardGenerator.kt:58` | **Critical** (privacy / Play policy) |
| Q11 | **AdMob test IDs shipped as constants** (`BANNER_TEST_ID`, etc.). These are the *public* Google test IDs; using them in production earns nothing and is against policy. | `ad/AdManager.kt:16-18`, `AndroidManifest.xml:18-20` | **Critical** |
| Q12 | **Self-contradictory signing config** — release build uses `signingConfig = signingConfigs.getByName("release")` whose keystore path defaults to `${rootDir}/my-upload-key.jks` (not in repo); README step 5 tells users to *remove* the debug signing line. Confusing + will break a fresh build. | `app/build.gradle.kts:23-49`, `README.md:20` | High |
| Q13 | **`fallbackToDestructiveMigration()`** silently wipes user data on any schema change. Currently DB is at `version = 5` with `exportSchema = false`, so there is no migration path and no schema history. | `data/database/AppDatabase.kt:30-31` | High |
| Q14 | **Magic numbers / unexplained constants** — e.g. `/12.0f` "Max logical variance delta ~ 12" (arbitrary, will clip progress for larger boards), `98765` seed salt, `delay(4000)` hint fade. | engine/viewmodel | Low |
| Q15 | **Fragile string serialization** of the board and history stack (`";"` / `","` / `"|"`). `deserializeBoard` calls `cols[c].toInt()` with no try/catch — a single corrupted row crashes the app on resume. | `GameViewModel.kt:114-140` | High |

### File-by-file observations

#### `MainActivity.kt`
- Duplicated DB/repository instantiation between `onCreate` and `onResume` (lines 38 vs 222).
- Music-start logic duplicated: `LaunchedEffect(userStats?.musicEnabled)` vs `onResume` block.
- Inline business logic: `bestTimeSeconds` recomputed from progress lists in the NavHost (lines 148–152) — belongs in the ViewModel.
- No `onDestroy` cleanup for `SoundEffectPlayer`'s `ToneGenerator`.
- **Medium.**

#### `GameViewModel.kt`
- `MutableStateFlow<Array<IntArray>?>` — emitting *the same mutable array reference* after mutating it (`boardState.value = currentBoard.deepCopy()` is fine, but `tapCell` mutates `currentBoard` in place before copying — safe only because of the copy).
- `init {}` launches multiple coroutines (streak, stats, save check, level load) — ordering between them is implicit.
- `isHapticEnabled()` defined and used; fine. But `playInteractionEffects` reads `userStats.value` which may be null on first tap (defaults to true) — acceptable.
- `startGameWithDifficulty` calls `writeActiveGameSave()` but `writeActiveGameSave` returns early if `boardState.value` is null — fine, but silently drops the save on race.
- Master-mode timer duplicated (Q7).
- `skipLevel()` writes a *fake* completion (1 star, 0 time) then loads next — players can cheese progression by skipping.
- **High.**

#### `LevelGenerator.kt`
- `Level` data class uses `Array<IntArray>` and correctly overrides `equals/hashCode` with `contentDeep*` — good.
- Recursive `generateWithParameters` for re-scramble has **no recursion bound** (`attempt` is unbounded); a pathological seed could recurse. In practice bounded by RNG, but unguarded.
- Deterministic per-level RNG (`Random(levelId)`) means **every install generates the identical level for a given id** — fine for daily parity, but "random" replay value is zero.
- **Medium.**

#### `GameSolver.kt`
- `getBestHintCell` clones the board *for every candidate cell* (O(n²) allocations per hint) and recomputes variance — acceptable for n≤5, but inefficient.
- Greedy variance heuristic does **not guarantee** a solvability hint; it can suggest a cell that increases the actual distance to solved. It is a heuristic, not a solver (despite the name).
- **Low–Medium.**

#### `DailyLeaderboardGenerator.kt`
- Misleadingly named `generateOnlineLeaderboardForDate` — **there is no network call**; it seeds an RNG to fabricate competitor names ("ZenPulse", "AetherFlow", …). Users are shown a fake "online" leaderboard. This is deceptive UX if shipped as multiplayer.
- Hardcoded developer email (Q10).
- **High** (deception + privacy).

#### `GameRepository.kt`
- `checkDailyStreak()` uses `Calendar.DAY_OF_YEAR` — **breaks on leap years / year rollovers** (`today == 1 && lastPlay >= 365` is a crude wrap heuristic). Off-by-one around Dec 31 → Jan 1.
- Every update method follows read-modify-write (`getOrCreateUserStats` then `insertOrUpdate`) — **race condition**: two concurrent writes (e.g., an undo firing while a reward fires) can clobber each other. No transaction/`@Update`.
- **Medium–High.**

#### `GameScreen.kt`
- ~1000-line composable; win/pause/tutorial/time-up dialogs nested inside the `if (winState)` block — the settings/pause/time-up/tutorial dialogs are *only reachable while the win dialog is also shown*? Actually they are inside the same `Box` under `if (winState)`, meaning **the pause-settings dialog and time-up dialog are gated behind the win overlay** — a structural UI bug: you cannot open in-game settings while a game is in progress unless you've already won. (See Phase 7.)
- `ConfettiOverlay`'s second `LaunchedEffect(particles)` re-enters `withFrameNanos` indefinitely until particles drain — fine, but it re-keys on the whole list each frame, causing recomposition churn.
- Casts `context as? Activity` for ad calls.
- **High.**

#### `HomeScreen.kt`
- Defines but never shows `DailyLeaderboardDialog` (Q3).
- Unused imports (Q4).
- Theme unlock logic re-declares `highestUnlockedLevel` twice (lines 61 and 560) — shadowing.
- `items(100)` hardcodes a 100-level grid; endless mode (levels >100) is unreachable from the UI.
- **Medium.**

#### `AnimatedTile.kt`
- `AnimatedContent` for number transitions is nice, but the whole tile rebuilds its color/border each recomposition; `pulseScale` loop runs forever while hinted.
- **Low.**

#### `AdManager.kt`
- Reasonable load/show lifecycle with `FullScreenContentCallback`.
- Test IDs (Q11).
- No retry/backoff on `onAdFailedToLoad`; relies on next `showX` to re-trigger load.
- **Medium** (production readiness).

#### `SoundEffectPlayer.kt`
- `ToneGenerator` is created once in `init {}` of a singleton — **never released**; leaks across the process and can throw on devices without the audio service (caught).
- `playSuccess` unused (Q6).
- **Medium.**

#### `AmbientMusicPlayer.kt`
- Raw `AudioTrack` streaming synth on a `Thread.MIN_PRIORITY` thread; `@Synchronized` start/stop.
- `stop()` interrupts and releases; decent. But `runPlayback` checks `Thread.currentThread().isInterrupted` and also `isPlaying` — double flag, and `audioTrack?.write` is a blocking call that may not honour interrupt promptly.
- If `start()` is called twice rapidly the `thread` field is overwritten before the old one exits — possible orphan thread (guarded mostly by `isPlaying`).
- **Medium.**

#### `Theme.kt` / `Color.kt` / `Type.kt`
- Six color schemes, cleanly switched by name.
- `Color.kt` still contains the default template `Purple80/Pink80/…` palette which is unused.
- `Type.kt` overrides only `bodyLarge`; the rest are template comments.
- **Low.**

### Anti-patterns summary

- God object (ViewModel, GameScreen).
- Feature envy / leaked platform deps (`Context` in ViewModel).
- Anemic "online" service that is actually local.
- Shotgun-surgery duplication (timer, formatters).
- Magic numbers.
- Commented-out code instead of deletion.
- Hardcoded PII.

### Circular dependencies
None detected at the package level (`ui → viewmodel → repository → database`; `engine` is leaf). The only intra-module cycle risk is conceptual: `GameScreen` ↔ `AdManager` and `GameViewModel` ↔ `SoundEffectPlayer`, both acyclic in practice.

### Code Quality Score: **5.5 / 10**
Functional and readable, but riddled with dead/duplicated code, two **critical** shipping blockers (PII + test ad IDs), and structural UI nesting bugs.

---

## Phase 4 — Runtime Stability Analysis

### Resource management
- `ToneGenerator` (singleton, never released) — **leak**.
- `AudioTrack` recreated on each start/stop and released in `stop()` — OK, but if `stop()` throws after `release()` the field is nulled safely.
- AdMob `InterstitialAd`/`RewardedAd` held in a singleton — fine.
- Room DB — singleton via double-checked locking — fine.

### Threading / Concurrency
- Game logic runs on the **main thread** (`MutableStateFlow` updates from `viewModelScope` which defaults to `Dispatchers.Main.immediate`). `LevelGenerator`/`GameSolver` are cheap; OK for grids ≤5.
- `AmbientMusicPlayer` uses a raw `Thread`, not a coroutine — minor inconsistency.
- **Race conditions** in `GameRepository` read-modify-write (Phase 3, Q in repo). Concurrent undo + reward + win-save can lose updates.
- `ConfettiOverlay` uses two `LaunchedEffect`s keyed on `size` and `particles`; the animation loop re-keys every frame — wasteful but not unsafe.

### Async operations
- Coroutines used throughout `viewModelScope`. No structured `withContext(Dispatchers.IO)` for DB calls — Room DAOs are `suspend` and dispatch on their own IO dispatcher by default, so this is acceptable.
- `AdManager.showInterstitial`/`showRewarded` invoke callbacks on the main thread via the SDK — OK.

### Event handling / State management
- `MutableStateFlow<Array<IntArray>?>` holds mutable arrays. `tapCell` mutates then deep-copies before emit; safe today but fragile if anyone holds a prior reference (the history stack pushes a deep copy too — good).
- `historyStack` is a plain `java.util.Stack` (not thread-safe) mutated from coroutines on the main dispatcher — safe while single-threaded, but not robust.

### Potential crash / freeze points

| Risk | Location | Likelihood | Impact |
|---|---|---|---|
| **`NumberFormatException` on resume** — corrupt `boardStateString`/`historyStackString` → `toInt()` throws | `GameViewModel.deserializeBoard` (line 122) | Medium (any schema/data corruption, OS kill mid-write) | **Crash on resume** |
| **Loss of in-memory game on process death** — `boardState` lives only in the ViewModel; auto-save is written on tap, not on timer tick or backgrounding. A user who backgrounds during MASTER mode loses time. | `GameViewModel` | Medium | Data loss, not crash |
| **`AudioTrack` init failure** — caught, returns silently; OK. `ToneGenerator` init failure — caught; OK. | sound/* | Low | Silent feature loss |
| **Vibrator cast / call on device without vibrator** — wrapped in try/catch; OK. Also uses deprecated `vibrate(long)` (no `VibratorManager`/API 26 `VibrationEffect`). | `GameViewModel.playInteractionEffects` | Low | Deprecation warnings; no-op on some devices |
| **Unbounded recursion in `LevelGenerator`** | engine | Very low | StackOverflow (theoretical) |
| **`AdManager.showInterstitial` on a finishing Activity** — calling `ad.show(activity)` after the activity is destroyed can throw; no isDestroyed check. | `AdManager.kt:76` | Low | Crash |
| **`fallbackToDestructiveMigration`** silently wipes data on a schema bump | `AppDatabase.kt:30` | Certain on next schema change | Data loss |

### Null-reference risks
- Generally well-handled: `boardState.value ?: return`, `userStats?.soundEnabled != false`, `stats?.musicEnabled != false`.
- `deserializeBoard` trusts its input — the one real NRE/parse-risk surface.

### Race conditions
- Repository read-modify-write (above).
- Two `viewModelScope.launch` blocks in `checkWinCondition` and `tapCell` could interleave if a user double-taps fast; `movesCount` is incremented synchronously before the launch, so score is consistent, but `writeActiveGameSave` + `deleteActiveGameSave` (on win) ordering depends on coroutine scheduling.

### Unhandled exceptions
- `try/catch (e: Exception)` covers audio and haptics but **swallows** errors with `printStackTrace()` only — no logging/crash reporting (no Firebase Crashlytics, no Timber).

### Stability Score: **6.0 / 10**
Defensive coding prevents most crashes; the real risks are (a) corrupt-save parse crash on resume, (b) data loss on process death / schema migration, and (c) the absence of any crash reporting.

---

## Phase 5 — Game Engine Analysis

There is **no third-party game engine**. The "engine" is a small pure-Kotlin module:

### Core game loop
- **Not a real-time loop.** The game is **turn-based / event-driven**: each `tapCell` mutates the board, recomputes state, and checks the win condition. There is no per-frame update of game state.
- The only continuous loops are: the **timer** (`delay(1000)`), the **hint pulse** (in `AnimatedTile`), and the **confetti** particle sim — all UI-side.

### Update / Render cycles
- Update = `tapCell` → board mutation → `boardState.value = ...` (Compose recomposes affected tiles).
- Render = Compose recomposition of `GameScreen` grid; `AnimatedTile` animates value changes via `AnimatedContent`.

### Physics / Input / Audio / Asset / Scene / Save-Load / Networking
- **Physics:** none.
- **Input:** Compose `clickable` per tile; no multi-touch/gestures.
- **Audio:** procedural (`ToneGenerator` SFX, `AudioTrack` music loop). No asset files.
- **Assets:** only launcher mipmaps; no art/audio assets — everything is generated at runtime. No asset pipeline.
- **Scene management:** Compose Navigation (3 screens). No scene graph.
- **Save/Load:** Room + custom string serialization (see Q15). Save-on-tap; load-on-resume.
- **Networking:** none (fake leaderboard notwithstanding).

### Engine architecture quality
- **Good:** stateless engine functions, deterministic level generation, clear data flow.
- **Weak:** the "engine" is smeared across `GameViewModel` (rules, scoring, timer, save) and `data/engine` (generation/solving). The *rules* of the game (the `±1` tap transformation) are duplicated in **four** places: `LevelGenerator.reverseTap`, `GameSolver.applyTap`, `GameViewModel.tapCell`, and implicitly described in the tutorial text. This is the single biggest engine-design flaw — **the core mechanic has no single source of truth**.

### Engine scalability / bottlenecks / performance
- Grid sizes 3–5 → trivial compute. `GameSolver.getBestHintCell` is O(n⁴) with allocations but n≤5 makes it negligible.
- Procedural audio thread runs continuously while music is on — minor CPU/battery cost.
- Confetti spawns 140 particles, each frame reallocating a new list via `mapNotNull` — brief GPU/CPU spike on win only.

### Engine design flaws / missing / over/under-engineered
- **Design flaw:** mechanic duplicated 4× (above). Also the win condition is "all cells equal" — but `calculateEquilibriumProgress` uses `max-min` range clipped to 12, which is inconsistent with the actual solvability metric and with the tutorial's "reach mathematical par" wording.
- **Missing:** no actual solver (the named `GameSolver` only picks a hint), so there's no way to verify a generated level is solvable in `par` moves. Levels are generated by *reverse-scrambling* from the solved state, so they are solvable in *at most* `totalMoves` — but `par` is set to `totalMoves`, which is the *scramble count*, not a proven optimal. Players can therefore often beat par.
- **Missing:** no undo of an *undo*; no move replay/share; no level editor.
- **Over-engineered:** the fake online leaderboard generator with deterministic "competitors".
- **Under-engineered:** save format (ad-hoc CSV) vs. using Room/Moshi properly (both are already dependencies).

### Is the engine production-ready?
**No.** It works as a prototype, but the duplicated mechanic, unverified par-optimality, fragile save format, and god-ViewModel orchestration need refactoring first.

### Game Engine Score: **6.0 / 10**

---

## Phase 6 — Library & Dependency Assessment

| Dependency | Version | Used? | Notes |
|---|---|---|---|
| AGP | 9.1.1 | yes | Very new; ensure CI/toolchain supports it. |
| Kotlin | 2.2.10 | yes | Recent; Compose compiler plugin is the modern path. ✓ |
| compose-bom | 2024.09.00 | yes | **Stale** — almost 2 years old as of 2026-06; many Material3 fixes since. |
| core-ktx | 1.18.0 | yes | OK. |
| lifecycle-* | 2.8.7 | yes | Slightly behind (2.9.x current). |
| activity-compose | 1.10.1 | yes | OK. |
| navigation-compose | 2.8.9 | yes | OK. |
| room-* | 2.7.0 | yes | OK; KSP codegen ✓. |
| kotlinx-coroutines-* | 1.10.2 | yes | OK. |
| **play-services-ads** | 23.3.0 | yes | OK; **but uses test ad units**. |
| **okhttp** | 4.10.0 | **NO** | **Outdated** (current 4.12/5.x); **unused** — no OkHttp usage in source. |
| **logging-interceptor** | 4.10.0 | **NO** | Unused; outdated. |
| **retrofit** | 2.12.0 | **NO** | Unused — no Retrofit interface exists. |
| **converter-moshi** | 2.12.0 | **NO** | Unused. |
| **moshi-kotlin** + **moshi-kotlin-codegen** | 1.15.2 | **NO** | Codegen via KSP active, but **no `@JsonClass` annotated class exists** — pure dead weight. |
| **firebase-bom** | 34.12.0 | **active impl, unused code** | BOM pulled in but **no Firebase module** (auth/ai/crashlytics) is implemented. Pure bloat. |
| accompanist-permissions | 0.37.3 | commented out | — |
| camera-* (×4) | 1.5.0 | commented out | — |
| datastore-preferences | 1.1.7 | commented out | — |
| coil-compose | 2.7.0 | commented out | (current Coil3 is `3.x`) |
| play-services-location | 21.3.0 | commented out | — |
| material-icons-extended | — | commented out | OK to keep commented. |
| robolectric | 4.16.1 | test only | OK. |
| roborazzi | 1.59.0 | test only | OK. |

### Redundant / conflicting / bloat
- **Unused active dependencies:** `okhttp`, `logging-interceptor`, `retrofit`, `converter-moshi`, `moshi-kotlin`, `moshi-kotlin-codegen`, `firebase-bom`. These add to APK/method count and attack surface for no benefit. **Recommendation:** remove them; the app needs none of them.
- **Version skew:** okhttp/interceptor pinned to 4.10.0 (old) while everything else is recent.
- **Compose BOM 2024.09.00** should be bumped to a 2026 BOM.
- No dependency conflict errors detected (versions are internally consistent per artifact).

### Security/maintenance concerns of deps
- OkHttp 4.10.0 has known CVEs in older 4.x lines — but since it's unused, just remove it.
- play-services-ads is fine; the risk is operational (test IDs), not the library.

### Recommendations
1. Remove the 7 unused active dependencies (retrofit/okhttp/moshi/firebase stack).
2. Bump Compose BOM, lifecycle, okhttp-if-readded.
3. Decide on Firebase: either remove the BOM or actually wire Crashlytics + the Gemini API the metadata claims (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`).
4. Keep Room, Coroutines, Navigation, AdMob.

---

## Phase 7 — UI/UX Audit

### User Interface

**Layout & visual hierarchy** — Generally strong: dark "premium neon" aesthetic, monospace titling, consistent `RoundedCornerShape`, generous use of cards with subtle borders. The home screen has a clear header → stats → resume banner → skins → daily → levels flow.

**Typography** — `FontFamily.Monospace` used heavily for a "techy" feel; body uses default. Sizes are hardcoded `sp` everywhere (no Material typography scale adoption) — inconsistent across screens.

**Color usage** — Six themes is a nice feature. However, many colors are **hardcoded** in composables (`Color(0xFF0F0F1E)`, `Color(0xFFD32F2F)`, `Color.Black`, `Color.White`) rather than drawn from `MaterialTheme.colorScheme`, so **themes don't fully apply** — e.g., the pause settings card (`GameScreen.kt:606`) is hardcoded `Color(0xFF0F0F1E)` regardless of selected theme. This undercuts the entire theme feature.

**Accessibility** — **Poor:**
- Many `fontSize = 9.sp` / `10.sp` / `11.sp` texts — below legibility thresholds.
- `contentDescription` is present on most icons (good), but several interactive `Box`/`Card` use `.clickable` with no semantic role or content description (e.g., theme skin cards, resume banner).
- No `minimumInteractiveComponentSize`/touch-target enforcement; some icon buttons are `36.dp` (below 48dp guideline).
- Color contrast: slate muted text (`#8B8C9E`) on dark surfaces may fail WCAG AA at small sizes.
- No support for system font scaling / `FontScale` is not explicitly respected (sp scales, but layouts may clip).
- No `TalkBack`-friendly semantics on the game grid (tiles show a number but no "row/col" semantics).

**Responsiveness** — Portrait phone layout only. Grid uses `aspectRatio(1f)` and weights — should adapt to most phone sizes, but **no landscape/tablet/foldable layout**; on a tablet the game grid would be a small centered box.

**Navigation** — `NavHost` with back stacks; `popUpTo("home")` on game start. Reasonable.

### User Experience

**Workflow** — Home → Difficulty → Game is clear. Resume banner is a nice touch. Settings accessible from Home header and (buggily) from in-game (see below).

**Ease of use / learnability** — A 3-step tutorial on level 1 — good. But the tutorial only appears on level 1 and the explanation of the actual mechanic is vague ("spread, absorb, and ripple values").

**Error recovery** — Undo/Reset/Skip exist. No error states to recover from (no network). Time-up modal in Master mode is clear.

**User feedback** — Haptics, tap tones, win confetti, animated tile bounce — **excellent** feedback layer.

**Interaction design** — Tap-to-balance is satisfying. Hint highlight pulses. Reward-ad gating is well-signposted.

### Identified UI bugs / UX problems

1. **In-game settings/pause/time-up dialogs are nested inside `if (winState)`** in `GameScreen` (`GameScreen.kt:474`–end). This means the pause-settings dialog and the Master "time's up" dialog are only structurally rendered while the win overlay is visible. **Practical effect:** the in-game settings button (`showPauseSettings`) and the time-up UI are effectively unreachable/latent. This is a structural compositing bug.
2. **Hardcoded colors ignore theme** (above) — themes only partially work.
3. **Endless levels unreachable** — `HomeScreen` renders `items(100)`; levels >100 exist in the engine but can't be selected.
4. **Fake "online" leaderboard** is deceptive; and the dialog that shows it is dead code anyway.
5. **`DailyLeaderboardDialog` never shown** despite the daily card existing.
6. **Hardcoded developer email** visible if the leaderboard dialog were ever wired up.
7. **`proGuard`/`minify`** disabled for release (`isMinifyEnabled = false`) → larger APK, no obfuscation.
8. Tutorial blocks taps except on `[1,1]`, which may confuse users who try another cell.
9. AdMob banner is always present at the bottom of the game screen, including during the win modal — visual clutter.
10. `SoundEffectPlayer`'s `playTap` plays even during tutorial/animated transitions — can stack tones.

### Screens needing redesign
- `GameScreen` — split dialogs out of the `winState` block; extract `ConfettiOverlay` to its own file.
- `HomeScreen` — wire or delete `DailyLeaderboardDialog`; fix theme application.

### Scores
- **UI: 6.5 / 10** — attractive aesthetic, but accessibility and theme-fidelity gaps.
- **UX: 6.0 / 10** — good feedback and flow, but deceptive leaderboard, latent dialogs, and unreachable content.

---

## Phase 8 — Performance Audit

### CPU
- Turn-based logic is trivial; `GameSolver` hint is the heaviest call (O(n⁴) allocs) and still sub-millisecond at n≤5.
- `ConfettiOverlay` reallocates the particle list every frame via `mapNotNull` — a per-frame allocation burst during the win modal (~few seconds). Minor.
- `AnimatedTile` `AnimatedContent` triggers slide/fade per value change — fine for 25 tiles max.

### GPU
- Pure Compose draw; `Canvas`-based confetti with `rotate` per particle (140×). Acceptable for a one-shot celebration.
- No shader/texture bottlenecks (no images).

### Memory
- Boards are tiny. History stack holds deep copies — bounded by moves per level (low).
- **Leaks:** `ToneGenerator` singleton never released; `AudioTrack` recreated each music start (released on stop). AdMob singletons persist (expected).
- Unused dependencies inflate APK size and dex count.

### Disk I/O
- Room queries are indexed only by `@PrimaryKey`; `getAllLevelProgress` returns all rows ordered — fine for ≤100 rows.
- Auto-save writes on every tap (`writeActiveGameSave`) → a DB write per tap. For a fast tapper this is many small writes/sec on the IO dispatcher. Not a bottleneck today, but wasteful; could debounce.

### Network
- None in practice. (Retrofit/OkHttp unused.)

### Startup / loading / cache
- **Startup:** `AdManager.initialize` + `loadInterstitial`/`loadRewarded` fire in `onCreate` (network-bound, async) — does not block UI. `AppDatabase.build()` is lazy until first DAO call. `ViewModel.init` loads level 1 synchronously on the main dispatcher (cheap).
- **Asset loading:** none (no assets).
- **Frame-rate stability:** no continuous render loop except music thread + brief confetti; should hold 60fps.

### Optimization recommendations
1. **Debounce auto-save** (e.g., save on tap but at most every 1s, plus on `onPause`).
2. **Cache the hint result** so rapid hint taps don't recompute.
3. **Remove unused deps** to shrink APK and cold-start class-loading.
4. **Release `ToneGenerator`** in `Activity.onDestroy`.
5. **Reuse particle list** in `ConfettiOverlay` (mutate in place) instead of `mapNotNull` per frame.
6. **Move level generation** to `Dispatchers.Default` defensively (already cheap).
7. Enable R8/`minify` for release.

### Performance Score: **6.5 / 10**

---

## Phase 9 — Security Assessment

### Input validation
- The only external inputs are: user taps (validated by grid bounds), settings toggles, and the **auto-save row** read back from DB. The save strings are parsed with `toInt()` and `split()` **with no validation** — a corrupted row (e.g., from a partial write or a hand-edited DB) crashes the app. (See Phase 4.)
- No user-generated text/UGC.

### Authentication / Authorization
- **None.** No login, no accounts, no server. (Yet the leaderboard pretends to be "online".)

### Secrets management
- `.env.example` declares `GEMINI_API_KEY`; the `secrets` Gradle plugin reads `.env` and exposes it as `BuildConfig`. **However, no code references `BuildConfig.GEMINI_API_KEY`** — the key is plumbed but unused. Good that it's not hardcoded; bad that the capability is advertised (`metadata.json`: `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`) but absent.
- Release signing secrets come from env vars (`STORE_PASSWORD`, `KEY_PASSWORD`) — acceptable. But the **debug keystore password is hardcoded** (`"android"`) and the release keystore path defaults to a file not in the repo.
- **`local.properties` (with `sdk.dir`) is committed** despite `.gitignore` listing it — mild info leak of a local path.

### API / Database security
- No API. Room DB is local-only, not exported, `allowBackup="true"` (so the DB can be pulled via adb/backup on rooted devices — game saves and the (fake) economy are unprotected). For a single-player game this is low-severity, but `allowBackup=true` plus no `dataExtractionRules` means the save DB is included in cloud backups unencrypted.

### Vulnerabilities / unsafe practices
- **PII hardcoded** in source (`tinupadikkalathomas@gmail.com`) — privacy violation, will be flagged by Play review if shown.
- **Test AdMob IDs in manifest** — policy violation if shipped to production.
- **No obfuscation/R8** on release — easier to reverse-engineer (low impact for this app).
- **No SSL/cert pining** — N/A (no networking).
- **`fallbackToDestructiveMigration`** — not a security bug, but it silently destroys data.
- **No ProGuard keep rules** for Room/Moshi/Compose — fine because minify is off; would need attention if enabled.

### Injection / data exposure
- No SQL injection (Room parameterized queries throughout — ✓).
- No injection surfaces.
- Data exposure: the cloud-backup of the unencrypted DB and the hardcoded email are the only real exposures.

### Severity ratings
| Finding | Severity |
|---|---|
| Hardcoded developer email in user-facing string | **Critical** |
| AdMob test ad IDs configured for release | **Critical** |
| Misleading "online"/server capability with no server | High (deception) |
| `allowBackup=true` + unencrypted save DB | Medium |
| Corrupt-save parse crash (input validation) | Medium |
| `local.properties` committed | Low |
| Unused Gemini secret plumbing | Low |

### Security Score: **4.0 / 10**
No network attack surface and good Room parameterization, but two **critical** policy/privacy issues plus the deceptive "online" framing pull it down hard.

---

## Phase 10 — Testing Assessment

### Test coverage
- **Effectively zero meaningful coverage.** The test sources are template stubs:
  - `ExampleUnitTest` — `assertEquals(4, 2 + 2)`.
  - `ExampleRobolectricTest` — asserts `app_name == "Equilibrium"`.
  - `GreetingScreenshotTest` — renders a single `AnimatedTile` and writes a Roborazzi PNG.
  - `ExampleInstrumentedTest` — asserts the package name.

### Unit / integration / E2E quality
- **Unit:** none for the engine, ViewModel, repository, or DAOs — the parts that *actually* need tests.
- **Integration:** none (no Room instrumentation tests, no DAO tests).
- **E2E/UI:** none (no Compose UI tests despite `compose-ui-test-junit4` being a dependency; no flows like play-a-level, win, undo, skip, daily challenge, resume are tested).

### Untested critical paths
- `LevelGenerator.generateLevel` / `generateDailyLevel` — determinism, solvability, "accidentally solved" recursion.
- `GameSolver.getBestHintCell` — correctness of the heuristic.
- `GameRepository.checkDailyStreak` — leap-year/year-rollover bug (would be caught by a test).
- `GameRepository.saveLevelProgress` / `saveDailyProgress` — best-time/stars merge logic.
- Board (de)serialization round-trip — the crash-on-corrupt-save risk.
- Win condition + star calculation.
- Timer / Master countdown.
- Auto-save resume.

### Fragile tests
- The screenshot test depends on Roborazzi/Robolectric + Compose versions; the stale BOM may cause flakiness.
- No tests to be fragile beyond that.

### Recommendations
1. Add unit tests for `LevelGenerator`, `GameSolver`, serialization round-trips, and streak math.
2. Add Room instrumentation tests for DAOs and repository race scenarios.
3. Add Compose UI tests for the core flow (Home → Difficulty → Game → Win → Next).
4. Add a property test that every generated level is solvable and that `par` is achievable.
5. Wire a CI (GitHub Actions) to run `./gradlew test check`.

### Testing Score: **2.5 / 10**
Template-only. The most logic-heavy code (engine + repository + serialization) has no tests at all.

---

## Phase 11 — Production Readiness Review

| Target | Verdict | Blockers |
|---|---|---|
| Personal use | ✅ Suitable | None (ignore the email/test-ad noise for personal use). |
| Internal use / demo | ⚠️ Mostly | Fix the latent in-game settings dialog bug; acceptable as a demo. |
| Public release (Play Store) | ❌ Not ready | Hardcoded PII; AdMob **test IDs** in manifest (policy violation); `allowBackup` leaks DB; no privacy policy; no crash reporting; misleading "online" leaderboard; no R8. |
| Commercial deployment | ❌ Not ready | All of the above, plus: ads earn $0 on test IDs, no analytics, no A/B, no IAP, fragile save format, no migration strategy, single module won't scale. |
| Large-scale deployment | ❌ Not ready | No backend, no multiplayer infra, no server-side validation, monolithic module. |

### Blockers preventing production release (must-fix)
1. **Remove hardcoded developer email** from `DailyLeaderboardGenerator.kt`.
2. **Replace AdMob test ad-unit IDs** with real ones (and gate by build type) — currently a Play policy violation.
3. **Fix `GameScreen` dialog nesting** so in-game settings and time-up are reachable.
4. **Add a privacy policy** and correct `dataExtractionRules`/`allowBackup` for the ad SDK and cloud-backup.
5. **Fix the daily-streak leap-year bug.**
6. **Validate the auto-save deserialization** (try/catch + schema versioning) to prevent resume crashes.
7. **Define a Room migration path** (remove `fallbackToDestructiveMigration`, add migrations, `exportSchema=true`).
8. **Remove unused dependencies** and bump Compose BOM.
9. **Add minimum crash reporting** (e.g., Firebase Crashlytics since the BOM is already pulled).
10. **Add tests** for engine + repository + serialization.

### Production Readiness Score: **3.5 / 10**

---

## Phase 12 — Technical Debt Report

### Architectural debt
- God `GameViewModel` and god `GameScreen`.
- UI ↔ AdManager direct coupling.
- No DI container.
- Single Gradle module.
- Fake "online" leaderboard masquerading as a service.

### Code debt
- Duplicated timer block, duplicated formatters, duplicated core mechanic (4×).
- Dead code: `DailyLeaderboardDialog`, `calculateEquilibriumProgress` (one of two), `playSuccess`, unused imports, unused deps.
- Hardcoded colors defeating the theme system.
- Magic numbers.

### Dependency debt
- 7 active-but-unused libraries.
- Stale Compose BOM (2024.09.00).
- Commented-out catalog entries kept "just in case".

### Documentation debt
- README is AI-Studio boilerplate only.
- No architecture doc, no LICENSE, no CONTRIBUTING, no CHANGELOG.
- The repo *contains the audit prompts* (`general_1.md`, `general_2.md`) and an AI Studio metadata file — template residue.

### Testing debt
- Effectively 0% meaningful coverage on logic-heavy code.

### Estimated refactoring effort
| Workstream | Effort |
|---|---|
| Split ViewModel + extract interfaces (DI) | 2–3 days |
| Unify the game mechanic + add solver/par verification | 1–2 days |
| Fix dialogs/themes/dead code/UI bugs | 2–3 days |
| Remove unused deps, bump BOM, enable R8 | 0.5 day |
| Real AdMob + privacy policy + Crashlytics | 1–2 days |
| Room migrations + save validation | 1 day |
| Test suite (unit + Room + Compose UI) | 3–5 days |
| **Total to production-ready** | **~2–3 engineer-weeks** |

### Maintenance cost
Low while single-developer and offline. Will rise sharply if multiplayer/cloud/IAP are added without refactoring the god objects and adding tests.

---

## Phase 13 — Final Verdict

### Top 10 strengths
1. Clean MVVM layering with a stateless, package-organized engine.
2. Procedural, deterministic level generation (good for daily parity).
3. Strong, cohesive visual identity with six themes.
4. Rich feedback layer (haptics, tones, animations, confetti).
5. Robust null-handling and try/catch around platform services.
6. Room-backed persistence with reactive `Flow`-based UI updates.
7. Auto-save / resume-session feature.
8. Well-structured AdMob preload/show lifecycle.
9. Procedural ambient music synth (no audio assets needed).
10. Hint system with a clear, pulsing UI affordance.

### Top 10 weaknesses
1. God `GameViewModel` (~540 LOC) mixing every concern.
2. God `GameScreen` (~1000 LOC) with **dialogs nested inside `if (winState)`** → latent UI.
3. Core game mechanic **duplicated in 4 places** — no single source of truth.
4. **Hardcoded developer email** in a user-facing string.
5. **AdMob test ad-unit IDs** shipped in manifest/constants.
6. **Fake "online" leaderboard** (deceptive; and its dialog is dead code).
7. Corrupt-save parse path will **crash on resume**.
8. `fallbackToDestructiveMigration` will silently **wipe user data**.
9. Dead code + 7 unused active dependencies + stale Compose BOM.
10. **No meaningful tests** and no CI.

### Top 10 risks
1. Play Store rejection / suspension over test ad IDs + PII.
2. Resume crash from a single corrupt save row.
3. Total progress loss on the next schema migration.
4. Daily-streak break/reset across year boundaries and leap years.
5. Race conditions in repository read-modify-write losing economy/progress updates.
6. Reputational risk from the deceptive "online" leaderboard.
7. Process-death data loss in Master timed mode.
8. `ToneGenerator`/`AudioTrack` failures on edge devices silently disabling audio.
9. Maintainer bottleneck: changing any rule means editing the god ViewModel and 4 duplicated mechanic sites.
10. No crash reporting → silent field failures.

### Top 10 improvement opportunities
1. Extract a single `GameRules` object owning the tap transformation; reuse in generator/solver/VM/tutorial.
2. Split `GameViewModel` into `EngineState`, `TimerController`, `SaveManager`, `Economy`.
3. Introduce Hilt/Koin DI and a `:core-engine`, `:data`, `:ui` module split.
4. Wrap AdMob behind an interface; inject; gate IDs by build variant.
5. Replace CSV board serialization with Moshi/Room (deps are already present) and validate on read.
6. Add real Room migrations + `exportSchema=true`.
7. Build a *real* backend (or honestly relabel the leaderboard as "simulated").
8. Add unit + integration + Compose UI tests + GitHub Actions CI.
9. Adopt Material typography scale and route all colors through `MaterialTheme.colorScheme` so themes fully apply.
10. Add Crashlytics + privacy policy + R8 and remove unused deps.

---

## Scores (Consolidated)

| Category | Score |
|---|---|
| Architecture | 6.5 / 10 |
| Code Quality | 5.5 / 10 |
| Stability | 6.0 / 10 |
| Game Engine | 6.0 / 10 |
| Performance | 6.5 / 10 |
| Security | 4.0 / 10 |
| UI | 6.5 / 10 |
| UX | 6.0 / 10 |
| Testing | 2.5 / 10 |
| Production Readiness | 3.5 / 10 |

---

## Crash Risk Assessment

**Overall: Low–Medium.**

*Why not High:* the app is almost entirely turn-based, offline, and defensively coded. The grid sizes are tiny, there's no networking, and platform-service failures (audio, vibrator) are caught. There are no obvious ANR loops (the only continuous loops are the 1s timer, the music synth, and the brief confetti).

*Why not pure Low:*
- The auto-save deserializer trusts its input; a single bad row → `NumberFormatException` → crash on "Resume".
- `fallbackToDestructiveMigration` + no migrations = data loss (not a crash, but a user-visible failure) on any schema bump.
- `AdManager.show*` on a destroyed Activity has an unchecked path.
- No crash reporting means real-world stability is unmeasured.

**Recommendation:** ship Crashlytics and harden the save path before public release; then crash risk drops to **Low**.

---

## Refactoring Priority List

### 1. Critical fixes (do before any external build)
- **C1** Remove hardcoded developer email from `DailyLeaderboardGenerator.kt:58`.
- **C2** Replace AdMob test IDs (`AdManager.kt:16-18`, `AndroidManifest.xml:18-20`) with real, build-variant-gated ad-unit IDs.
- **C3** Validate auto-save deserialization (`GameViewModel.deserializeBoard/deserializeHistory`) — try/catch + fallback to fresh level.
- **C4** Add a privacy policy and set correct `allowBackup` / `dataExtractionRules`.

### 2. High-priority fixes
- **H1** Unify the tap mechanic into one `GameRules` object; remove the 3 other copies.
- **H2** Split `GameViewModel` and extract a testable `TimerController` + `SaveManager`.
- **H3** Move the pause/time-up/tutorial dialogs **out of** `if (winState)` in `GameScreen`.
- **H4** Replace `fallbackToDestructiveMigration` with real migrations + `exportSchema=true`.
- **H5** Fix `checkDailyStreak` leap-year / year-rollover logic.
- **H6** Add crash reporting (Crashlytics) and remove the unused Retrofit/OkHttp/Moshi/Firebase dead weight.

### 3. Medium-priority fixes
- **M1** Route hardcoded `Color(0xFF…)` through `MaterialTheme.colorScheme`.
- **M2** Wire or delete `DailyLeaderboardDialog`; relabel the "online" leaderboard honestly.
- **M3** Debounce `writeActiveGameSave`; release `ToneGenerator` on destroy.
- **M4** Add unit tests for engine, serialization round-trip, streak, and star/progress math.
- **M5** Bump Compose BOM and lifecycle; enable R8 with proper keep rules.
- **M6** Make `items(100)` endless-aware or cap the engine's endless mode.

### 4. Low-priority fixes
- **L1** Remove dead code (`calculateEquilibriumProgress` duplicate, `playSuccess`, unused imports).
- **L2** Adopt Material typography scale; enforce ≥48dp touch targets; add `TalkBack` semantics to tiles.
- **L3** Add landscape/tablet layouts.
- **L4** Replace deprecated `Vibrator.vibrate(Long)` with `VibrationEffect` / `VibratorManager`.
- **L5** Add LICENSE, CONTRIBUTING, architecture README; remove `general_*.md` template residue.
- **L6** Improve `GameSolver` to a real solver that verifies `par` optimality.

---

*End of report. This audit was produced by exhaustive file-by-file inspection of every Kotlin source file, all Gradle/config files, the manifest, resources, and the test sources in the repository.*
