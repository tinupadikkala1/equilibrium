# Project Evolution Plan — Equilibrium

> **This is the single source of truth (SSOT).** Any AI agent or human resuming work on Equilibrium should read this file first. It explains the current state, the problems, the strategy, priorities, architecture decisions, product vision, and roadmap — and points to the detailed sibling documents for execution.

**Last updated:** 2026-06-21
**Companion documents (read alongside this):**
1. `PROJECT_AUDIT_REPORT.md` — the audit that motivates everything here.
2. `TECHNICAL_REFACTORING_PLAN.md` — architecture + code-quality refactoring + **critical remedies (incl. AdMob build-variant gating that preserves test IDs)**.
3. `PERFORMANCE_AND_STABILITY_PLAN.md` — performance targets, crash/root-cause analysis, "runs continuously" checklist.
4. `UI_UX_IMPROVEMENT_PLAN.md` — UI/UX redesign + accessibility + implementation/validation checklists.
5. `PRODUCT_IMPROVEMENT_ROADMAP.md` — product goals, ethical engagement/retention, week-by-week milestones.
6. `AI_AGENT_TASK_BACKLOG.md` — every task (TASK-001…TASK-032), testing plan, security remediation, production-readiness checklist.

---

## 1. What is Equilibrium?

Equilibrium is a **single-module Kotlin / Jetpack Compose Android puzzle game**. The core mechanic: tap a cell → it loses 1, each orthogonal neighbour gains 1; the goal is to make every cell on the grid equal ("equilibrium"). It features:
- Procedurally generated, deterministic levels (3×3 → 5×5) plus an endless mode.
- Daily challenge with a (currently *simulated*, not real) leaderboard.
- Two difficulty modes: Zen (untimed, unlimited hints) and Master (timed, limited hints).
- Six visual themes, haptics, procedural tones, confetti win celebration.
- Undo / Hint / Skip economy backed by Room (SQLite).
- Auto-save / resume.
- Google AdMob monetization: **banner** (game bottom), **interstitial** (every 5 level wins), **rewarded** (when out of undo/hint/skip).

The project was generated from an **AI Studio template** and is functional as a prototype but is **not yet production-ready**.

---

## 2. Current Project State (snapshot)

| Dimension | State |
|---|---|
| Languages / stack | Kotlin (JVM 11), Jetpack Compose (Material 3), Room, Navigation-Compose, Coroutines, AdMob. |
| Size | ~18 hand-written Kotlin files, ~2.5k LOC, single Gradle module (`:app`). |
| DB | Room `equilibrium_database`, version 5, 4 tables, `fallbackToDestructiveMigration`. |
| Tests | Template stubs only (`2+2`, app-name, one screenshot). No real coverage. |
| CI/CD | None. |
| Backend | None (Retrofit/OkHttp/Moshi/Firebase-BOM are present but **unused**). |
| Ad system | Present and functional, using **Google test ad IDs**. |
| Known defects | 2 Critical, several High — see §3. |

**Audit scores (from `PROJECT_AUDIT_REPORT.md`):**
Architecture 6.5 · Code Quality 5.5 · Stability 6.0 · Game Engine 6.0 · Performance 6.5 · Security 4.0 · UI 6.5 · UX 6.0 · Testing 2.5 · Production Readiness 3.5.
**Crash risk:** Low–Medium.

---

## 3. Existing Problems (the "why")

### Critical (block any public release)
1. **Hardcoded developer email** (`tinupadikkalathomas@gmail.com`) in a user-facing leaderboard string (`DailyLeaderboardGenerator.kt:58`) — PII leak + store-policy issue.
2. **AdMob test ad-unit IDs shipped in manifest/constants** (`AdManager.kt`, `AndroidManifest.xml`) — Play policy violation; earns nothing.
3. **Corrupt-save crash** — `deserializeBoard` calls `toInt()` on untrusted input with no try/catch → app crashes on "Resume" if the save row is malformed.
4. **In-game Settings/Time-up/Tutorial dialogs unreachable mid-game** — they are nested inside `if (winState)` in `GameScreen`.

### High
- Core game mechanic **duplicated in 4 places** (no single source of truth).
- **God `GameViewModel`** (~540 LOC) and **god `GameScreen`** (~1000 LOC).
- **UI directly couples to `AdManager`** (casts `context as? Activity`).
- **`fallbackToDestructiveMigration`** will silently wipe user data on next schema change.
- **Daily-streak bug** across leap years / year boundary.
- **Repository race conditions** (read-modify-write not transactional).
- **Deceptive "online" leaderboard** (it's local RNG).
- **Ad can be shown on a destroyed Activity** → crash risk.
- **7 active-but-unused dependencies**; stale Compose BOM.

### Medium / Low
Dead code (`DailyLeaderboardDialog`, `playSuccess`, unused imports), duplicated formatters/timer block, hardcoded colors bypassing themes, accessibility gaps (9–11 sp text, <48 dp targets, missing semantics), `items(100)` hides endless mode, `ToneGenerator` leak, magic numbers, no CI, no Crashlytics.

> Full evidence with file/line references: `PROJECT_AUDIT_REPORT.md`.

---

## 4. Improvement Strategy (the "how")

Guided by twelve primary goals (G1–G12) — fix all critical/high issues; improve architecture, performance, UI/UX, stability, scalability, code quality, testing; prepare for production; **preserve ad functionality**; increase ethical retention.

### Strategy pillars
1. **Stabilize first** (Critical fixes M0) — no new features until the app can't crash or leak PII.
2. **Refactor behind a stable API** — split internals but keep the Compose/ViewModel surface unchanged until replacements are tested.
3. **Single-source-of-truth the mechanic** — one `GameRules` object.
4. **Preserve ads exactly** — wrap `AdManager` behind `AdController`; keep test IDs for debug; gate real IDs to release.
5. **Test the riskiest code first** — engine, serialization, repository, save/resume.
6. **Measure, don't guess** — performance targets, crash-free sessions, coverage gate.
7. **Ethical engagement** — opt-in, value-first, no dark patterns.

---

## 5. Architecture Decisions (ADRs, condensed)

| # | Decision | Rationale |
|---|---|---|
| AD-1 | Introduce `data/engine/GameRules.kt` as the **only** place the ±1 mechanic lives. | Eliminates the 4-way duplication that risks behavioural drift. |
| AD-2 | Split `GameViewModel` into `GameEngineState` + `TimerController` + `SaveManager` + `EconomyController`. | Each < 150 LOC, testable, single-responsibility. |
| AD-3 | Wrap ads in `interface AdController`; UI never references `AdManager` directly. | Decouples UI from the ad SDK; enables mocking; **does not change ad behavior**. |
| AD-4 | Build-variant-gated AdMob IDs: `defaultConfig` keeps Google **test IDs**; `release` overrides from `.env`/env and `error()`s if absent. | Resolves the policy violation **without removing test IDs** (constraint honored). |
| AD-5 | Hilt DI replacing manual `ViewModelProvider.Factory` wiring. | Removes duplicated graph construction; enables test fakes. |
| AD-6 | Multi-module split: `:core-engine` (pure Kotlin), `:core-data` (Room), `:ad`, `:ui`, `:app`. | Enforced layer boundaries; faster builds; engine unit-testable without Android. |
| AD-7 | Real Room migrations + `exportSchema=true`; remove `fallbackToDestructiveMigration`. | Preserve user data across upgrades. |
| AD-8 | Store streak as `lastPlayEpochDay`; drop `DAY_OF_YEAR`. | Correct across leap years and year rollover. |
| AD-9 | Relabel the leaderboard as **"Practice Rivals (simulated)"** (or build a real backend later). | Honesty; removes deception risk. |
| AD-10 | Crashlytics + Timber; R8 on release. | Field visibility; smaller/safer release APK. |

---

## 6. Task Priorities (summary — full list in `AI_AGENT_TASK_BACKLOG.md`)

| Priority | Tasks |
|---|---|
| **Critical** | TASK-001 (email), TASK-003 (AdMob gating), TASK-004 (save validation), TASK-005 (dialog hoist). |
| **High** | TASK-006 (`GameRules`), 007 (VM split), 008 (`AdController`), 009 (`HapticController`), 012 (unused deps), 014 (migrations), 015 (streak fix), 016 (atomic repo), 021 (ad guard), 025 (honest leaderboard), 029 (Crashlytics). |
| **Medium** | 002 (display name), 010 (split `GameScreen`), 011 (Hilt), 013 (BOM bump), 017 (debounce save), 018 (tone leak), 023 (theme tokens), 024 (a11y), 026 (endless UI), 027 (achievements), 028 (streak rewards), 030 (reminders), 032 (R8). |
| **Low** | 019 (confetti reuse), 020 (hint cache), 022 (recursion bound), 031 (multi-module). |

---

## 7. Product Vision

**One-line vision:** *The most satisfying 5-minute daily puzzle on Android — fair, honest, beautiful, and rewarding to master.*

**Pillars:**
- **Fair:** no dark patterns, no deceptive "online" claims, ads never gate core progression.
- **Honest:** labels match reality (simulated leaderboard is labeled as such).
- **Beautiful:** six fully-applied themes, polished motion, accessible by default.
- **Rewarding to master:** real achievements, skill-based scoring, gentle daily habit.
- **Monetizable without harm:** ads preserved and working; future IAP only for cosmetics.

---

## 8. Roadmap (milestones & weeks)

```
M0  Critical stabilization .............. Week 1
M1  Structural refactor ................. Weeks 1–2
M2  Stability & performance ............. Week 3
M3  UI/UX polish + engagement ........... Weeks 3–4
M4  Production preparation .............. Week 4
```

| Week | Deliverables |
|---|---|
| **Week 1** | TASK-001, 003, 004, 005 closed (PII gone; ads gated but test IDs preserved; corrupt-save safe; Settings reachable). Start TASK-006 (`GameRules`) + TASK-012 (dep cleanup). |
| **Week 2** | Split `GameViewModel` (007); `AdController`/`HapticController` (008/009); split `GameScreen` (010); Hilt (011); atomic repo (016); ad guard (021); BOM bump (013). Engine + repo unit tests green. |
| **Week 3** | Real migrations (014); streak fix (015); debounce autosave (017); tone leak (018); confetti/hint optimizations (019/020); recursion bound (022); Crashlytics (029); R8 (032). Performance targets met; "runs continuously" checklist passes. |
| **Week 4** | Theme tokens (023); accessibility (024); honest leaderboard (025); endless UI (026); achievements (027); streak rewards (028); opt-in reminders (030); multi-module (031); display name (002). Production-readiness checklist ticked; signed release build. |

> 2 engineers can compress this to ~2.5 weeks.

---

## 9. Hard Constraints (non-negotiable for any agent)

1. **Ads:** never remove, disable, or relocate. Debug must keep serving Google **test ad IDs**. Release uses real IDs via build-variant gating (TASK-003). Placement count and triggers stay identical.
2. **Privacy:** no PII in source; honest UI labels; `allowBackup` hardened.
3. **Ethics:** all engagement features opt-in and value-first; no dark patterns.
4. **Continuity:** every refactor keeps the existing public Compose/ViewModel API stable until its replacement is tested.
5. **No silent data loss:** `fallbackToDestructiveMigration` must go (TASK-014).

---

## 10. How to Resume Work (for the next agent)

1. Read this file (done).
2. Open `AI_AGENT_TASK_BACKLOG.md`; pick the lowest-numbered open Critical/High task whose dependencies are satisfied.
3. Read the relevant sibling plan section for design detail.
4. Implement; add/extend tests per the Testing Plan.
5. Update the task status in `AI_AGENT_TASK_BACKLOG.md` and add a regression test for the bug you fixed.
6. Verify against the task's **Verification steps**; do not mark done on intent alone.
7. For any architecture change, record it as an ADR in §5.

---

## 11. Definition of Done (project-level)

The productization effort is complete when **all** are true:
- All Critical and High tasks closed and verified.
- Production-readiness checklist (`AI_AGENT_TASK_BACKLOG.md`) fully ticked.
- Performance targets (`PERFORMANCE_AND_STABILITY_PLAN.md` Part D) met.
- "Runs continuously" checklist (Part C) passes on API 24/29/33/36.
- ≥ 70% line coverage on `:core-engine` + `:core-data`; key flows E2E-tested.
- Ad system behavior unchanged (debug=test IDs, release=real IDs, placements identical).
- No PII or test ad IDs leak into a release build.
- Signed release build uploaded to an internal test track.

---

*End of master plan. For any detail, follow the links to the sibling documents.*
