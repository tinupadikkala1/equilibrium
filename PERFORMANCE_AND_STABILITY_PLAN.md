# Performance & Stability Plan

**Goal:** Make Equilibrium fast, responsive, leak-free, and crash-free — and prove it with measurable targets and checklists.
**Inputs:** `PROJECT_AUDIT_REPORT.md` Phases 4 & 8.

---

## Part A — Performance Optimization

### A1. Startup optimization
**Current:** `onCreate` calls `AdManager.initialize` (async, fine) + builds Room DB (lazy until first DAO use) + `GameViewModel.init` loads level 1 on the main dispatcher.
**Target:** cold start to first frame < 800 ms on a mid-range device (Android Go baseline).
**Tasks:**
| Task | Target | Notes |
|---|---|---|
| Defer `AdManager.initialize` to `Application.onCreate` via Hilt eager singleton (off the Activity path) | -100 ms Activity onCreate | Don't block UI thread. |
| Preload only level-1 board; lazy-load the rest | already done | — |
| Move `repository.checkDailyStreak()`/`getOrCreateUserStats()` off the init critical path (fire-and-forget) | -50 ms | They don't gate first paint. |
| Remove unused deps (Retrofit/OkHttp/Moshi/Firebase BOM) | -1–2 MB dex, faster class loading | See TECH plan. |
| Enable R8/`isMinifyEnabled = true` for release with proper keep rules | -20–40% APK, faster cold start | Add `-keep` for Room entities, AdMob, Moshi (if re-added). |

### A2. Memory optimization
**Issues:** `ToneGenerator` singleton never released (leak); `AudioTrack` recreated each start (released on stop — OK); confetti reallocates list every frame.
**Tasks:**
| Task | Target |
|---|---|
| Release `SoundEffectPlayer.toneGen` in `Activity.onDestroy` (or Application teardown) | No audio-service leak. |
| Reuse confetti particle list (mutate in place, filter dead indices) instead of `mapNotNull` per frame | ~140 allocs/frame → 0. |
| Cap `historyStack` depth (e.g., last 50 boards) | Bounded memory on long sessions. |
| Lazy-init AdMob `InterstitialAd`/`RewardedAd` only after first level complete | Lower early RSS. |

### A3. CPU optimization
**Issues:** `GameSolver.getBestHintCell` is O(n⁴) with per-cell allocation; `AnimatedContent` per tile; confetti `rotate` per particle.
**Tasks:**
| Task | Target |
|---|---|
| Cache the last hint result; invalidate only on board change | 1 hint compute per board state. |
| Allocate one reusable temp board in `GameSolver` instead of cloning per candidate | O(n²) fewer allocs. |
| Profile with Android Studio CPU profiler on a 5×5 Master level; ensure no jank > 16 ms frame | 60 fps target. |

### A4. Rendering optimization
**Tasks:**
| Task | Target |
|---|---|
| Mark tiles `@Stable`/wrap board in a stable holder to minimize recomposition | Only the changed tile recomposes. |
| Use `derivedStateOf` for `progress`/`isSolved` in `GameScreen` | Avoid recompute per recomposition. |
| Move `ConfettiOverlay` to a `Surface`/separate composition scope | Don't invalidate the grid. |

### A5. Asset optimization
There are no image/audio assets (everything procedural) — so this is minimal.
**Tasks:** Replace `.webp` launcher mipmaps with optimized/sized variants if needed; verify no oversized drawables via `apkanalyzer`.

### A6. Database optimization
**Issues:** `writeActiveGameSave` writes to Room on **every tap**; no indexes beyond PK.
**Tasks:**
| Task | Target |
|---|---|
| Debounce autosave (write at most every 1.5 s + always on `onPause`/win) | DB writes/sec from "every tap" → ≤ 1/s. |
| Add indexes on `level_progress.completed`, `daily_challenge_progress.dateKey` if list queries grow | Future-proof. |
| Use a single transaction for `clearAllProgressAndResetStats` | Atomic reset. |
| Replace `fallbackToDestructiveMigration` with real migrations + `exportSchema=true` | No data loss. |

### A7. Network optimization
No network today. If the "online" leaderboard becomes real:
- Use OkHttp + connection pool (already in deps), gzip, ETag caching; mock via `MockWebServer` in tests.

---

## Part B — Stability Improvement

For each issue: **root cause → solution → verification.**

### B1. Crash on resume from corrupt save
- **Root cause:** `deserializeBoard` calls `toInt()` on untrusted input (`GameViewModel.kt:114-124`).
- **Solution:** wrap in try/catch, return null, fall back to `loadLevel(1)` (see TECH plan `CRIT-3`).
- **Verify:** unit test with `"1,2;3,X"` → no throw, returns null.

### B2. Data loss on schema migration
- **Root cause:** `fallbackToDestructiveMigration()` + `exportSchema = false` (`AppDatabase.kt:30`).
- **Solution:** implement `Migration(5 → 6, ...)`, set `exportSchema = true`, commit schema JSON.
- **Verify:** instrumented test runs v5 → v6 with a seeded v5 DB and asserts rows survive.

### B3. Daily-streak bug at year boundary / leap years
- **Root cause:** uses `Calendar.DAY_OF_YEAR` (`GameRepository.kt:142-164`); Dec 31 → Jan 1 logic is a crude heuristic that also mishandles leap years.
- **Solution:** store `lastPlayEpochDay = LocalDate.now().toEpochDay()`; compare `today - lastPlay == 1`.
- **Verify:** unit tests for: same day, next day, skipped day, year rollover, leap day.

### B4. Race conditions in repository read-modify-write
- **Root cause:** `getOrCreateUserStats` then `insertOrUpdate` without a transaction; concurrent undo+reward can clobber.
- **Solution:** wrap each economy op in a Room `@Transaction` (or `withTransaction { }`); or use an atomic `UPDATE user_stats SET hints = hints + :n`.
- **Verify:** instrumented test fires N concurrent `addHints(1)` and asserts final count == N.

### B5. Process-death data loss (Master timed mode)
- **Root cause:** timer state lives only in the ViewModel; autosave doesn't include `secondsElapsed`+`countdownSecondsLeft` at backgrounding.
- **Solution:** persist `secondsElapsed` on `onPause` (already partly serialized); re-derive `countdownSecondsLeft = baseTime - secondsElapsed` on resume (already done in `resumeActiveGame` — verify coverage).
- **Verify:** instrumented test backgrounds the app mid-Master and resumes with correct remaining time.

### B6. Ad shown on destroyed Activity
- **Root cause:** `AdManager.showInterstitial`/`showRewarded` don't check `activity.isDestroyed`.
- **Solution:** guard `if (activity.isFinishing || activity.isDestroyed) { onClosed(); return }`.
- **Verify:** rotate device during ad load → no crash.

### B7. Audio service failure on edge devices
- **Root cause:** `ToneGenerator`/`AudioTrack` can throw on devices without the service.
- **Solution:** already caught (good); add a feature-flag so UI silently disables sound if init fails once.
- **Verify:** Robolectric test with audio service unavailable.

### B8. Unbounded recursion in LevelGenerator
- **Root cause:** `generateWithParameters` re-scrambles recursively without a bound.
- **Solution:** cap `attempt <= 10`; if exceeded, fall back to a fixed default scramble.
- **Verify:** unit test with adversarial seed.

### B9. Resource leak — ToneGenerator
- Covered in A2.

### B10. State management — duplicate MVVM sources
- **Root cause:** `MainActivity` recomputes `bestTimeSeconds` from progress lists (lines 148–152) instead of the ViewModel.
- **Solution:** expose `bestTimeSeconds` as a `StateFlow` from `GameViewModel`.
- **Verify:** UI test asserts the value updates after a win.

---

## Part C — "Runs continuously without crashing" Checklist

Run this before every release. Each item must pass on: API 24, 29, 33, 36; small (3 GB RAM) and large devices.

- [ ] Cold launch → Home renders in < 1 s, no ANR.
- [ ] Play 100 consecutive taps on a 5×5 Master level — no jank, no OOM.
- [ ] Win → confetti → Next Stage, repeated 20× — heap does not grow > 20%.
- [ ] Background the app mid-Master → kill process → relaunch → Resume restores board + timer.
- [ ] Corrupt the active-game-save row (manual DB edit) → Resume falls back gracefully (no crash).
- [ ] Undo with empty history → no-op (no crash).
- [ ] Hint with no `hints` left → falls through to rewarded ad or disabled state (no crash).
- [ ] Skip with no `skips` left → falls through to rewarded ad (no crash).
- [ ] Rotate device 10× during a game → state preserved, no duplicate timers.
- [ ] Toggle Sound/Music/Haptic rapidly → audio threads start/stop cleanly, no orphan threads.
- [ ] Reset All Data → DB cleared, stats reset, level 1 reloaded.
- [ ] Daily challenge across a simulated year boundary (via injected clock) → streak increments correctly.
- [ ] AdMob load fails (airplane mode) → app remains usable; placements retry on next show.
- [ ] AdMob interstitial/rewarded shown and dismissed 5× → no leak, no crash.
- [ ] No `IllegalArgumentException` / `NullPointerException` in a 30-min monkey run.

---

## Part D — Measurable Performance Targets

| Metric | Current (est.) | Target | How to measure |
|---|---|---|---|
| Cold start to first frame | ~1.2 s | < 800 ms | `Debug.startMethodTracing` / macrobenchmark |
| Frame drop rate during play | < 1% | 0 jank frames | Perfetto / on-device profiler |
| Heap after 20 wins | unknown | < +20% steady | Android Studio memory profiler |
| DB writes/sec during fast tapping | up to ~8/s | ≤ 1/s (debounced) | Room query log |
| APK size (release) | ~8–10 MB (with unused deps) | −2 MB after dep cleanup + R8 | `apkanalyzer` |
| Crash-free sessions | unknown | ≥ 99.5% | Firebase Crashlytics (post-prod) |

---

## Part E — Security Remediation (cross-link)

(Prioritized severity — details in `PROJECT_AUDIT_REPORT.md` Phase 9, expanded in `AI_AGENT_TASK_BACKLOG.md`.)
1. **Critical** — Remove hardcoded email (CRIT-1, done in TECH plan).
2. **Critical** — Build-variant AdMob IDs (CRIT-2, done in TECH plan; test IDs preserved).
3. **High** — Honest "simulated" leaderboard label (no deceptive multiplayer).
4. **Medium** — `allowBackup=false` for the Room DB, or encrypt via SQLCipher; correct `dataExtractionRules`.
5. **Medium** — Validate all deserialized save data (CRIT-3).
6. **Low** — Remove committed `local.properties`; remove unused OkHttp (drops CVE surface).
7. **Low** — Add R8 obfuscation for release.
