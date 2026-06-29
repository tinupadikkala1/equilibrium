# Product Improvement & Engagement/Retention Roadmap

**Goal:** Transform Equilibrium from a functional prototype into a stable, scalable, polished, monetizable, production-ready product — *without* harming UX and *without* touching the ad system's behavior or test IDs.
**Inputs:** `PROJECT_AUDIT_REPORT.md` + the four sibling plans.

This document covers **product goals, engagement & retention mechanisms, the phased roadmap, and week-by-week milestones**. Technical depth lives in the sibling plans; this is the *product* view.

---

## 1. Primary Goals (acceptance criteria)

| # | Goal | How we know it's met |
|---|---|---|
| G1 | Fix all critical issues | CRIT-1..CRIT-4 closed & verified (see TECH plan). |
| G2 | Fix all high-priority issues | All "High" items in `AI_AGENT_TASK_BACKLOG.md` closed. |
| G3 | Improve architecture & maintainability | `:core-engine` is pure Kotlin; `GameViewModel` < 150 LOC. |
| G4 | Improve performance | All targets in `PERFORMANCE_AND_STABILITY_PLAN.md` Part D met. |
| G5 | Improve UI & UX | All items in UI implementation + validation checklists ticked. |
| G6 | Improve stability | "Runs continuously" checklist (PERF plan Part C) passes on 4 API levels. |
| G7 | Improve scalability | Multi-module build; real backend option stubbed (or leaderboard honestly relabeled). |
| G8 | Improve code quality | 0 dead code flagged by detekt/ktlint; A1–A8 resolved. |
| G9 | Improve testing coverage | ≥ 70% line coverage on `:core-engine` + `:core-data`; key flows E2E-tested. |
| G10 | Prepare for production | Production-readiness checklist (backlog) fully ticked. |
| G11 | **Preserve ad functionality** | Banner/Interstitial/Rewarded placements unchanged in count & trigger; debug still serves test IDs. |
| G12 | Engagement without harm | Retention features ship **opt-in**, no dark patterns. |

---

## 2. Product Quality Targets

Stable · Fast · Responsive · Maintainable · Extensible · User-friendly · Visually appealing · Scalable · Monetizable · Production-ready.

(Mapped to G1–G12 above; each is operationalized in the sibling plans with measurable targets.)

---

## 3. User Engagement & Retention Plan

**Design principle:** every mechanism below is *opt-in, honest, value-first*. No dark patterns, no manipulation, no nagging.

For each: **Purpose · User benefit · Implementation complexity · Expected retention impact.**

### 3.1 Progress tracking
- **Purpose:** Make growth visible.
- **User benefit:** Sense of advancement; clear "what next".
- **Complexity:** Low (data already in Room).
- **Retention impact:** Medium. Visible progress bars + total-stars + levels-unlocked on Home.

### 3.2 Achievement system (new)
- **Purpose:** Recognize skill milestones (e.g., "3-star 10 levels", "Solve a 5×5 under par", "7-day streak").
- **User benefit:** Goals to chase beyond just "next level".
- **Complexity:** Medium (new `Achievement` entity + check-on-win hook + UI).
- **Retention impact:** Medium-high. Long-tail goals extend engagement.
- **Ethics:** Achievements reward *skill/consistency*, not spend.

### 3.3 Streak system (exists — fix + extend)
- **Purpose:** Reward daily return.
- **User benefit:** Free hints/undos for maintaining streak (currently streak is display-only).
- **Complexity:** Low–Medium (fix year-boundary bug; add streak→reward ladder).
- **Retention impact:** High (proven pattern), but **no loss-aversion pressure** — we never threaten to "lose" a streak; we only reward keeping one.
- **Ethics:** No "streak in danger!" notifications.

### 3.4 Usage reminders (opt-in notifications)
- **Purpose:** Bring users back gently.
- **User benefit:** A daily challenge reminder they asked for.
- **Complexity:** Medium (`WorkManager` + `NotificationManager`; runtime permission on Android 13+).
- **Retention impact:** Medium.
- **Ethics:** Off by default; user picks time; one tap to silence; no manipulative copy.

### 3.5 Reward systems (already partly present via rewarded ads)
- **Purpose:** Free currency for engaged users.
- **User benefit:** Earn hints/undos/skips by watching a rewarded ad **or** by achieving milestones — never paywalled-only.
- **Complexity:** Low (extend `earnReward`).
- **Retention impact:** Medium.
- **Ethics:** Reward ads are *optional*; never gate core progression behind them. Skip, Hint, Undo are conveniences, not necessities.

### 3.6 Personalization
- **Purpose:** Make it feel like *yours*.
- **User benefit:** 6 themes (have), plus user display name, plus per-theme tile shapes later.
- **Complexity:** Low (name) → Medium (shapes).
- **Retention impact:** Low–Medium.

### 3.7 Smart recommendations
- **Purpose:** Surface the right next challenge.
- **User benefit:** "You're close to 3-starring Stage 12 — one more try?" or "Try Master mode on a stage you've already beaten".
- **Complexity:** Medium (heuristic over `LevelProgress`).
- **Retention impact:** Medium.

### 3.8 Meaningful notifications
- Same channel as 3.4; content is *informational*: "New daily challenge is live", "You set a new best time on Stage 8".
- **Ethics:** No FOMO, no deception.

### 3.9 Content progression
- **Purpose:** Always something new.
- **User benefit:** Endless procedural levels (engine supports it; UI caps at 100 — fix).
- **Complexity:** Low (raise/remote `items(100)` cap; paginate).
- **Retention impact:** Medium.

### 3.10 Habit-forming workflows (ethical)
- Daily challenge + streak + gentle reminder = a healthy "5-minute daily puzzle" habit loop.
- **Ethics:** Bounded sessions (no infinite-scroll-style engagement); the game *ends* each level; we don't exploit variable-reward schedules to manufacture compulsion.

### Retention impact summary (ranked)
1. Streak→reward ladder (High) 2. Achievements (Med-High) 3. Daily challenge reminder (Med) 4. Endless content (Med) 5. Smart recommendations (Med) 6. Personalization (Low-Med).

---

## 4. Phased Roadmap (milestone-based)

### Milestone M0 — Critical stabilization (Week 1)
- CRIT-1 email removal; CRIT-2 build-variant AdMob IDs (test IDs preserved); CRIT-3 save validation; CRIT-4 GameScreen dialog hoist.
- **Exit criteria:** no PII in source; debug build still shows test ads; release build fails without real IDs; Settings reachable mid-game; corrupt-save no longer crashes.
- **Ad check:** banner/interstitial/rewarded placements & triggers unchanged.

### Milestone M1 — Structural refactor (Weeks 1–2)
- `GameRules` (A2); split `GameViewModel` (A1); `AdController`/`HapticController` (A4/A5); split `GameScreen` files (rest of A3); Hilt DI (A6).
- **Exit criteria:** `:core-engine` extractable; `GameViewModel` < 150 LOC; UI no longer references `AdManager` directly; engine unit tests green.
- **Ad check:** `AdController` wraps `AdManager` unchanged; behavior identical.

### Milestone M2 — Stability & performance (Week 3)
- Real Room migrations + `exportSchema=true`; debounce autosave; fix streak bug; atomic repo ops; release `ToneGenerator`; reuse confetti buffer; cache hints; enable R8.
- **Exit criteria:** PERF plan Part C checklist passes; Part D targets met.

### Milestone M3 — UI/UX polish + engagement (Weeks 3–4)
- Theme token adoption; accessibility pass; honest leaderboard relabel; achievements; streak→reward; endless-level UI; opt-in reminders.
- **Exit criteria:** UI validation checklist passes; ≥ 1 new ethical retention feature shipped and E2E-tested.

### Milestone M4 — Production preparation (Week 4)
- Multi-module split (A7); CI; Crashlytics; privacy policy; `dataExtractionRules`; store listing assets; final production-readiness checklist.
- **Exit criteria:** Production-readiness checklist (backlog) fully ticked; release build signed with real keystore + real ad IDs.

---

## 5. Week-by-Week Plan

### Week 1 — Critical fixes + start of refactor
- Day 1–2: CRIT-1, CRIT-2 (with ad preservation verified), CRIT-3, CRIT-4.
- Day 3–5: `GameRules` extraction + engine unit tests; remove unused deps.

### Week 2 — Architectural split
- Split `GameViewModel` → engine/timer/save/economy; extract `AdController`/`HapticController`; Hilt DI; split `GameScreen` files.
- Add integration tests for repository atomicity.

### Week 3 — Stability + performance
- Room migrations; streak fix; debounced autosave; resource-leak fixes; R8; performance profiling vs targets.

### Week 4 — UI/UX + engagement + production
- Accessibility + theme tokens; honest leaderboard; achievements + streak rewards; opt-in reminders; multi-module split; CI; Crashlytics; store readiness.

> Effort estimate assumes ~1 engineer; parallelize with 2 engineers to compress to ~2.5 weeks.

---

## 6. Guardrails (non-negotiable)

1. **Ads:** never remove/disable/relocate; debug keeps test IDs; release uses real IDs via build-variant gating.
2. **Privacy:** no PII in source; honest labels; `allowBackup` hardened.
3. **Ethics:** engagement features are opt-in, value-first, no dark patterns.
4. **Continuity:** every refactor keeps the existing public Compose/ViewModel API stable until its replacement is tested.
