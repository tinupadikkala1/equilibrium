# AI-Agent Task Backlog

**Purpose:** A complete, dependency-aware, machine-and-human-readable backlog. Every unit of work in the Equilibrium productization effort is a task here with an ID, priority, dependencies, expected outcome, and verification steps.
**Status legend:** `[ ]` not started · `[~]` in progress · `[x]` done.
**Hard constraint:** No task may remove, disable, or relocate the ad system. Test ad IDs are preserved (debug); real IDs are added via build-variant gating (release).

---

## TASK-001 — Remove hardcoded developer email
- **Title:** Strip PII from leaderboard string
- **Description:** In `DailyLeaderboardGenerator.kt:58` the user's label is `"You (tinupadikkalathomas@gmail.com)"`. Replace with `"You"` (or a `displayName` column — see TASK-002).
- **Priority:** Critical
- **Dependencies:** —
- **Expected outcome:** No developer PII anywhere in source.
- **Verification:** `grep -rn "tinupadikkalathomas" app/` → no matches.

## TASK-002 — (Optional) Add user `displayName`
- **Title:** User-configurable display name
- **Description:** Add `displayName: String = "Player"` to `UserStats`; Room migration v5→v6; show in Settings; use in leaderboard label.
- **Priority:** Medium
- **Dependencies:** TASK-001, TASK-014
- **Expected outcome:** Personalized, non-PII label.
- **Verification:** Migration test passes; name persists across reinstall-less upgrade.

## TASK-003 — Build-variant-gated AdMob IDs (preserve test IDs)
- **Title:** Real ad IDs for release; test IDs retained for debug
- **Description:** Implement the scheme in `TECHNICAL_REFACTORING_PLAN.md` §3 CRIT-2: `BuildConfig` fields default to Google test IDs in `defaultConfig`; `release` build type overrides with values from `.env`/env vars and `error()`s if missing; `AdManager` reads `BuildConfig`; `AndroidManifest` uses `${ADMOB_APP_ID}` placeholder.
- **Priority:** Critical
- **Dependencies:** —
- **Expected outcome:** Debug build serves test ads (unchanged); release build uses real IDs or fails to build; no test-ID leak to production.
- **Verification:**
  - Debug APK: ad requests target `ca-app-pub-3940256099942544/*`.
  - `./gradlew assembleRelease` without `.env` → fails with `ADMOB_APP_ID missing for release`.
  - `./gradlew assembleRelease` with `.env` → real IDs in `BuildConfig` + manifest.
  - Ad placements (banner at game bottom, interstitial every 5 wins, rewarded for undo/hint/skip) unchanged in count and trigger.

## TASK-004 — Validate auto-save deserialization
- **Title:** Crash-safe resume
- **Description:** Wrap `deserializeBoard`/`deserializeHistory` in try/catch returning null; `resumeActiveGame` falls back to `loadLevel(1)` + user toast on corruption.
- **Priority:** Critical
- **Dependencies:** —
- **Expected outcome:** Corrupt save row never crashes the app.
- **Verification:** Unit test `"1,2;3,X"` → null; instrumented test corrupts row and resumes without crash.

## TASK-005 — Hoist GameScreen dialogs out of `if (winState)`
- **Title:** Make in-game Settings/Time-up/Tutorial reachable
- **Description:** Move `showPauseSettings`, `isTimeUp`, `tutorialActive` dialog blocks to be siblings of the win dialog (outside the win `Box`).
- **Priority:** Critical
- **Dependencies:** —
- **Expected outcome:** Mid-game Settings opens; Time-up modal appears in Master mode; Tutorial shows on level 1.
- **Verification:** UI test taps Settings mid-game → dialog shown.

## TASK-006 — Extract `GameRules` single source of truth
- **Title:** Unify the ±1 tap mechanic
- **Description:** Create `data/engine/GameRules.kt` with `applyTap`, `applyReverseTap`, `isSolved`, `equilibriumProgress`. Replace the 4 duplicated implementations.
- **Priority:** High
- **Dependencies:** —
- **Expected outcome:** Mechanic defined once; behaviour can't drift.
- **Verification:** `grep -rn "dr\[0\] == -1\|board\[r\]\[c\] -= 1\|+= 1" app/src/main` → only in `GameRules.kt`; engine tests pass.

## TASK-007 — Split `GameViewModel`
- **Title:** Decompose god ViewModel
- **Description:** Extract `GameEngineState`, `TimerController`, `SaveManager`, `EconomyController`; `GameViewModel` becomes a thin facade.
- **Priority:** High
- **Dependencies:** TASK-006
- **Expected outcome:** Each collaborator < 150 LOC and independently testable; public VM API unchanged.
- **Verification:** `MainActivity`/NavHost compile unchanged; unit tests for each collaborator.

## TASK-008 — Extract `AdController` interface
- **Title:** Decouple UI from AdMob
- **Description:** `interface AdController` wrapping the existing `AdManager` (no behavior change); inject into ViewModel; Composable calls `vm.*` instead of `AdManager.*`.
- **Priority:** High
- **Dependencies:** TASK-003
- **Expected outcome:** UI no longer casts `context as? Activity` for ads; ad layer mockable.
- **Verification:** `grep -n "AdManager\." app/src/main/java/com/example/ui/` → no matches; ad behavior unchanged via UI test.

## TASK-009 — Extract `HapticController`
- **Title:** Remove `Context` from ViewModel
- **Description:** `interface HapticController` + `AndroidHapticController`; inject; remove `context` param from `tapCell`.
- **Priority:** High
- **Dependencies:** TASK-007
- **Expected outcome:** ViewModel is pure Kotlin (no `Context`).
- **Verification:** `grep -n "Context" GameViewModel.kt` → no matches.

## TASK-010 — Split `GameScreen` into files
- **Title:** Decompose god composable
- **Description:** Move `ConfettiOverlay`, `WinDialog`, `PauseSettingsDialog`, `TimeUpDialog`, `TutorialOverlay` into `ui/screens/game/`.
- **Priority:** Medium
- **Dependencies:** TASK-005
- **Expected outcome:** Each file < 250 LOC; previewable.
- **Verification:** Roborazzi screenshot parity before/after.

## TASK-011 — Introduce Hilt DI
- **Title:** Replace manual wiring
- **Description:** `@HiltAndroidApp`, modules for DB/DAO/repo/AdController/HapticController/Clock; `@HiltViewModel`; `@AndroidEntryPoint MainActivity`.
- **Priority:** Medium
- **Dependencies:** TASK-007, TASK-008, TASK-009
- **Expected outcome:** No manual `ViewModelProvider.Factory`; test fakes injectable.
- **Verification:** App builds & boots; UI smoke passes.

## TASK-012 — Remove unused dependencies
- **Title:** Trim dependency bloat
- **Description:** Remove (or wire) Retrofit, OkHttp, logging-interceptor, Moshi, Moshi-codegen, Firebase BOM (unless Crashlytics added — see TASK-029).
- **Priority:** High
- **Dependencies:** —
- **Expected outcome:** Smaller APK; less attack surface.
- **Verification:** `./gradlew :app:dependencies` shows them gone; build green.

## TASK-013 — Bump Compose BOM & lifecycle
- **Title:** Update stale libraries
- **Description:** Compose BOM 2024.09.00 → latest 2026 BOM; lifecycle 2.8.7 → 2.9.x.
- **Priority:** Medium
- **Dependencies:** —
- **Expected outcome:** Bug fixes; deprecation warnings cleared.
- **Verification:** Build green; screenshot parity.

## TASK-014 — Real Room migrations
- **Title:** Stop destroying user data on schema change
- **Description:** Remove `fallbackToDestructiveMigration`; set `exportSchema=true`; commit schema JSON; add `Migration(5→6)` as needed by TASK-002.
- **Priority:** High
- **Dependencies:** —
- **Expected outcome:** Upgrades preserve data.
- **Verification:** Instrumented test seeds v5 DB → upgrades → rows survive.

## TASK-015 — Fix daily-streak year-boundary bug
- **Title:** Correct streak math
- **Description:** Store `lastPlayEpochDay` (`LocalDate.toEpochDay()`); compare deltas; remove `DAY_OF_YEAR` heuristic.
- **Priority:** High
- **Dependencies:** TASK-014
- **Expected outcome:** Streaks correct across leap years and Dec 31 → Jan 1.
- **Verification:** Unit tests for same-day, next-day, skipped, year rollover, leap day.

## TASK-016 — Atomic repository operations
- **Title:** Eliminate read-modify-write races
- **Description:** Wrap economy ops in `@Transaction` or use atomic SQL `UPDATE … SET col = col + :n`.
- **Priority:** High
- **Dependencies:** —
- **Expected outcome:** Concurrent undo+reward+win can't clobber counts.
- **Verification:** Instrumented test fires N concurrent `addHints(1)` → final == N.

## TASK-017 — Debounce autosave
- **Title:** Reduce DB write pressure
- **Description:** Save at most every 1.5 s, plus always on `onPause`/win.
- **Priority:** Medium
- **Dependencies:** TASK-007
- **Expected outcome:** DB writes ≤ 1/s during fast tapping.
- **Verification:** Room query log during a fast-tap session.

## TASK-018 — Release `ToneGenerator`
- **Title:** Fix audio-service leak
- **Description:** Release `SoundEffectPlayer.toneGen` on `Activity.onDestroy` (or via lifecycle observer).
- **Priority:** Medium
- **Dependencies:** —
- **Expected outcome:** No leaked `ToneGenerator`.
- **Verification:** LeakCanary clean across 20 win/play cycles.

## TASK-019 — Confetti buffer reuse
- **Title:** Cut per-frame allocations
- **Description:** Mutate the particle list in place; filter dead indices instead of `mapNotNull` per frame.
- **Priority:** Low
- **Dependencies:** TASK-010
- **Expected outcome:** ~0 allocations/frame during win.
- **Verification:** Memory profiler shows no transient list spike.

## TASK-020 — Cache hint result
- **Title:** Avoid recomputing hints
- **Description:** Cache last `getBestHintCell` per board state; invalidate on tap.
- **Priority:** Low
- **Dependencies:** TASK-006
- **Expected outcome:** One hint compute per board.
- **Verification:** Unit test asserts compute called once per state.

## TASK-021 — Guard ad show on destroyed Activity
- **Title:** Prevent ad crash
- **Description:** In `AdManager.show*`, guard `if (activity.isFinishing || activity.isDestroyed) { onClosed(); return }`.
- **Priority:** High
- **Dependencies:** —
- **Expected outcome:** No crash on config change during ad.
- **Verification:** Rotate device during ad load → no crash.

## TASK-022 — Bound LevelGenerator recursion
- **Title:** Prevent theoretical stack overflow
- **Description:** Cap `attempt <= 10`; fall back to a fixed scramble.
- **Priority:** Low
- **Dependencies:** —
- **Expected outcome:** No unbounded recursion.
- **Verification:** Unit test with adversarial seed → terminates.

## TASK-023 — Route hardcoded colors through theme
- **Title:** Make themes apply everywhere
- **Description:** Replace `Color(0xFF…)`/`Color.Black`/`Color.White` with `MaterialTheme.colorScheme.*`.
- **Priority:** Medium
- **Dependencies:** —
- **Expected outcome:** All six themes fully apply.
- **Verification:** Screenshot tests across 6 themes; no hardcoded color leak.

## TASK-024 — Accessibility pass
- **Title:** WCAG AA + TalkBack
- **Description:** ≥ 48 dp touch targets; tile/card semantics; contrast fixes; dynamic-type respect; reduce-motion path.
- **Priority:** Medium
- **Dependencies:** —
- **Expected outcome:** Passes accessibility checklist.
- **Verification:** TalkBack walkthrough; contrast scanner; 130% font scale test.

## TASK-025 — Honest leaderboard label
- **Title:** Remove deceptive "online" framing
- **Description:** Rename `generateOnlineLeaderboardForDate` → `generateSimulatedLeaderboard`; relabel UI "Practice Rivals (simulated)"; either wire `DailyLeaderboardDialog` to a route or delete it.
- **Priority:** High
- **Dependencies:** TASK-001
- **Expected outcome:** No deceptive multiplayer claim.
- **Verification:** UI text review; no "online" claim without a real backend.

## TASK-026 — Endless-level UI
- **Title:** Unhide endless mode
- **Description:** Replace `items(100)` with a paged `LazyVerticalGrid` so levels >100 are reachable.
- **Priority:** Medium
- **Dependencies:** —
- **Expected outcome:** All generated levels selectable.
- **Verification:** Scroll to level 150 and start it.

## TASK-027 — Achievements system
- **Title:** Ethical long-tail goals
- **Description:** New `Achievement` entity; check-on-win hook; UI screen; rewards (cosmetic/themes).
- **Priority:** Medium
- **Dependencies:** TASK-016
- **Expected outcome:** ≥ 10 achievements; E2E test for one.
- **Verification:** Win a level → achievement unlocks.

## TASK-028 — Streak→reward ladder
- **Title:** Reward daily consistency
- **Description:** Grant hints/undos for maintaining streaks (no loss-aversion).
- **Priority:** Medium
- **Dependencies:** TASK-015
- **Expected outcome:** Streak yields in-game currency.
- **Verification:** Simulate 7-day streak → reward granted.

## TASK-029 — Crashlytics + logging
- **Title:** Field visibility
- **Description:** Add Firebase Crashlytics (re-enable BOM for this alone); add Timber for structured logging.
- **Priority:** High
- **Dependencies:** TASK-012
- **Expected outcome:** Crash-free metric visible; logs attributable.
- **Verification:** Force a test exception → appears in Crashlytics console.

## TASK-030 — Opt-in reminders (notifications)
- **Title:** Gentle daily return
- **Description:** `WorkManager` + notification (runtime permission on 13+); user-configurable time; off by default.
- **Priority:** Medium
- **Dependencies:** —
- **Expected outcome:** One daily reminder if user opted in.
- **Verification:** Opt-in → notification fires; opt-out → silent.

## TASK-031 — Multi-module split
- **Title:** `:core-engine`, `:core-data`, `:ad`, `:ui`, `:app`
- **Description:** Move packages into modules; `:core-engine` has zero Android deps.
- **Priority:** Low
- **Dependencies:** TASK-006, TASK-007, TASK-008, TASK-011
- **Expected outcome:** Enforced layer boundaries; faster builds.
- **Verification:** `:core-engine` builds without `android` plugin.

## TASK-032 — Enable R8 / minify for release
- **Title:** Shrink + obfuscate
- **Description:** `isMinifyEnabled = true`; add keep rules (Room entities, AdMob, Compose).
- **Priority:** Medium
- **Dependencies:** TASK-012
- **Expected outcome:** −20–40% APK; no runtime stripping errors.
- **Verification:** Release build runs all flows; ad SDK intact.

---

## Testing Plan (roadmap)

### Unit tests (`:core-engine`, `:core-data`)
- `GameRules.applyTap` / `applyReverseTap` are inverses (property test).
- `LevelGenerator`: determinism, solvability (reverse-scrambled boards are solvable), "accidentally solved" recursion bounded (TASK-022).
- `HintEngine` (renamed `GameSolver`): heuristic returns a valid in-bounds cell; cache invalidation.
- Serialization round-trip + corruption-safe (TASK-004).
- `GameRepository`: streak math (TASK-015); best-time/stars merge; atomic ops (TASK-016).
- Star/progress calculation; win condition.

### Integration tests (instrumented)
- Room DAOs: insert/query/delete across all 4 tables.
- Repository: concurrent writes (TASK-016); migration v5→v6 (TASK-014).
- Auto-save round-trip via `SaveManager`.

### End-to-end (Compose UI)
- Home → Difficulty → Game → Win → Next Stage.
- Undo/Reset/Hint/Skip paths; rewarded-ad fallback (mock `AdController`).
- Resume from save; corrupt save fallback.
- Daily challenge flow; achievements unlock; streak reward.
- Master mode time-up dialog (TASK-005).

### Regression tests
- For each closed Critical/High task, add a regression test encoding the old bug:
  - Resume with corrupt save → no crash.
  - Year-boundary streak → correct.
  - Mid-game Settings reachable.
  - Ad placements unchanged (count + trigger).
  - No PII in source.

### Tooling
- JUnit4 + Robolectric (already deps) + Compose UI test + Roborazzi screenshots.
- Add **ktlint**, **detekt**, **LeakCanary** (debug).
- Coverage gate: ≥ 70% on `:core-engine` + `:core-data`.

### Testing roadmap (sequenced)
1. Engine unit tests (TASK-006). 2. Serialization + repo tests. 3. Room migration/integration tests. 4. Compose UI E2E. 5. Regression suite per closed task. 6. CI wiring.

---

## Security Remediation (prioritized)

| # | Item | Severity | Task |
|---|---|---|---|
| S1 | Hardcoded developer email | Critical | TASK-001 |
| S2 | AdMob test IDs in release | Critical | TASK-003 (gated, preserved for debug) |
| S3 | Deceptive "online" leaderboard | High | TASK-025 |
| S4 | Corrupt-save crash (input validation) | Medium | TASK-004 |
| S5 | `allowBackup=true` + unencrypted DB | Medium | (below) |
| S6 | No R8 obfuscation | Low | TASK-032 |
| S7 | Committed `local.properties` | Low | (below) |
| S8 | Unused OkHttp CVE surface | Low | TASK-012 |

**S5 task:** set `android:allowBackup="false"` (or encrypt DB via SQLCipher) and populate `dataExtractionRules` to exclude the DB from cloud backup.
**S7 task:** `git rm --cached local.properties`; confirm `.gitignore` entry exists (it does).

(No authentication/authorization/API concerns — the app is offline today. If a real backend is added, add auth via Firebase Auth + server-side validation; tracked as future work, not part of this baseline.)

---

## Production Readiness Checklist

### Build verification
- [ ] `./gradlew clean assembleDebug assembleRelease` both succeed.
- [ ] Release build requires real ad IDs (TASK-003) or fails.
- [ ] R8 enabled (TASK-032); APK size within target.
- [ ] Versioning: bump `versionCode`/`versionName` per release.

### Deployment verification
- [ ] Signed release APK/AAB with real keystore (env-supplied secrets).
- [ ] Internal test track upload succeeds.

### Crash monitoring & logging
- [ ] Crashlytics integrated (TASK-029).
- [ ] Timber logging in place; no PII logged.
- [ ] Crash-free sessions ≥ 99.5% after soak test.

### Analytics
- [ ] (Optional) Firebase Analytics or self-hosted; events: level_start, level_win, hint_use, ad_shown (no PII).

### Advertisement verification (constraint)
- [ ] Banner placement present at game bottom (unchanged).
- [ ] Interstitial triggers every 5 level wins (unchanged).
- [ ] Rewarded triggers for out-of-undo/hint/skip (unchanged).
- [ ] Debug build still serves Google **test ad IDs** (preserved).
- [ ] Release build serves real IDs from `.env`.
- [ ] Ad load failures degrade gracefully (no crash, no error toast).

### Performance verification
- [ ] All targets in `PERFORMANCE_AND_STABILITY_PLAN.md` Part D met.
- [ ] "Runs continuously" checklist (Part C) passes on API 24/29/33/36.

### Security verification
- [ ] S1–S8 above resolved.
- [ ] No secrets in source (`grep` for keys/emails clean).
- [ ] Privacy policy published; `dataExtractionRules` correct.

### User acceptance verification
- [ ] Usability test: new user solves level 1 without help.
- [ ] Accessibility test: TalkBack + 130% font + reduce motion pass.
- [ ] No deceptive UI copy (leaderboard labeled honestly).

---

## Milestone → Task mapping

- **M0 (Week 1, Critical):** TASK-001, 003, 004, 005.
- **M1 (Weeks 1–2, Structural):** TASK-006, 007, 008, 009, 010, 011, 012, 013, 016, 021.
- **M2 (Week 3, Stability/Perf):** TASK-014, 015, 017, 018, 019, 020, 022, 029, 032.
- **M3 (Weeks 3–4, UI/UX + Engagement):** TASK-023, 024, 025, 026, 027, 028, 030.
- **M4 (Week 4, Production):** TASK-002, 031, plus full Production Readiness Checklist.
