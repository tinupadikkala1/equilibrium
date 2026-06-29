# UI/UX Improvement Plan

**Goal:** Make Equilibrium visually consistent, accessible, responsive, and genuinely pleasant to use — without harming the existing flow or the ad experience.
**Inputs:** `PROJECT_AUDIT_REPORT.md` Phase 7.

---

## 1. Current UX/UI State (recap)

**Strengths to preserve:** dark "premium neon" aesthetic; six themes; haptics + tones + confetti feedback; auto-save/resume banner; daily event; difficulty modes; tutorial; clear star/par HUD.

**Weaknesses to fix:**
- In-game Settings / Time-Up dialogs are nested inside `if (winState)` → **unreachable mid-game** (structural bug).
- Hardcoded `Color(0xFF…)` bypasses `MaterialTheme.colorScheme` → themes only partially apply.
- Accessibility gaps: 9–11 sp text, sub-48 dp touch targets, missing TalkBack semantics on tiles/cards.
- `DailyLeaderboardDialog` defined but never shown; leaderboard is fake-"online" (deceptive).
- `items(100)` hard-cap hides the endless mode.
- No landscape/tablet/foldable layout.

---

## 2. UI Improvement Plan

### 2.1 Layout consistency
- Replace all inline `Color(0xFF…)` / `Color.Black` / `Color.White` with `MaterialTheme.colorScheme.*` tokens so all six themes apply uniformly.
- Define a spacing token set (`Spacing.xs=4dp, sm=8, md=12, lg=16, xl=24`) — eliminate scattered literal paddings.
- Standardize card shape (`RoundedCornerShape(16.dp)` body, `20.dp` hero) via a `Shapes` object in `theme/`.

### 2.2 Visual hierarchy
- Establish a Material3 type ramp (`MaterialTheme.typography`): replace ad-hoc `fontSize` values with `headlineLarge/Medium/Small`, `titleMedium`, `bodyMedium`, `labelSmall`.
- Cap minimum body text at 12 sp; labels at 11 sp only for purely decorative monospace tags.

### 2.3 Navigation
- Keep 3-route NavHost but: (a) make Settings reachable from anywhere via a top-level icon, (b) add a `leaderboard` route once the dialog is wired (or remove the dialog), (c) ensure back-stack correctness (already uses `popUpTo("home")`).

### 2.4 Accessibility (target: WCAG AA contrast, basic TalkBack)
| Task | Detail |
|---|---|
| Touch targets ≥ 48 dp | Enforce `minimumInteractiveComponentSize` or wrap small icon buttons. |
| Tile semantics | Each tile exposes `contentDescription = "Row ${r+1}, Column ${c+1}, value $value, tap to balance"`. |
| Theme skin cards | Add `Modifier.semantics { contentDescription = "Theme $name, ${if (locked) "locked, reach stage $req" else "tap to select"}" }`. |
| Contrast | Ensure muted text `#8B8C9E` on dark surfaces passes AA; if not, lighten to `#A0A1B3` for body text. |
| Focus order | Verify Tab/arrow traversal with TalkBack on Home, Difficulty, Game. |
| Dynamic type | Respect `FontScale`; test at 130% — clip/scroll where needed. |
| Reduce motion | Honour `Settings.Global.ANIMATOR_DURATION_SCALE`/`LocalAccessibilityManager` to disable confetti + tile bounce for users who turn animations off. |
| Color-blind | Balance progress uses color hue + a numeric %, so it's not color-only — good; keep it. |

### 2.5 Readability
- Tutorial copy is vague ("spread, absorb, ripple"). Rewrite: *"Tap a tile: it loses 1, each neighbour gains 1. Make every tile show the same number."* Add a 3-frame animated diagram.

### 2.6 Responsiveness
- Add `WindowSizeClass` support: phone portrait (current), phone landscape (grid on left, HUD on right), tablet/foldable (expanded two-pane Home).
- Grid already uses `aspectRatio(1f)` + weights → should adapt; verify on 7" and 10" emulators.

### 2.7 UI redesign recommendations
- **Home:** keep structure; add a "Continue" CTA prominence; surface Achievements preview (see Product plan).
- **Game:** move ad banner so it never overlaps the win modal; add a subtle "moves vs par" progress bar.
- **Settings:** consolidate into one screen reachable from Home and in-game (post-CRIT-4 fix).
- **Win modal:** add "Share result" + "Daily rank" (once leaderboard is honest).

### 2.8 UI implementation checklist
- [ ] `theme/Tokens.kt` (colors, shapes, spacing) created; all composables use tokens.
- [ ] All `Color(0xFF…)` removed from screens.
- [ ] Typography scale adopted.
- [ ] Touch targets ≥ 48 dp; semantics added.
- [ ] Settings/Time-up/Tutorial dialogs hoisted out of `if (winState)` (CRIT-4).
- [ ] `WindowSizeClass` layouts for phone/landscape/tablet.
- [ ] `DailyLeaderboardDialog` either wired to a route or deleted.
- [ ] Endless-level grid (`LazyVerticalGrid` paging) replaces `items(100)`.

### 2.9 UI validation checklist
- [ ] Screenshot tests (Roborazzi) for Home/Game/Difficulty across all 6 themes × 3 sizes.
- [ ] TalkBack walkthrough passes on all screens.
- [ ] Contrast checker passes AA on all text.
- [ ] 130% font scale does not clip critical text.
- [ ] Landscape + tablet render without overlap.
- [ ] Reduce-motion path disables confetti.

---

## 3. UX Improvement Plan

### 3.1 Onboarding
- Replace 3-step text tutorial with a **1-step interactive demo**: highlight center cell, prompt tap, show the ripple, then auto-advance. Add a "Skip tutorial" and a replay entry in Settings.
- First-run: gentle prompts for Sound/Music permission toggles (no system permissions needed today).

### 3.2 Ease of use
- Single-tap to play (already). Add long-press on a tile to preview its neighbours' deltas (teaches the rule visually).
- "Hint" should briefly annotate *why* a cell is suggested (e.g., "this cell is furthest from balance").

### 3.3 Error handling
- No network errors today. Add: graceful degradation when AdMob fails (no error toast to user; just retry silently).
- Validate save corruption → user-friendly "We couldn't resume your last session; starting fresh." toast (CRIT-3).

### 3.4 Feedback systems
- Already strong. Add: subtle haptic on win (different from tap); a soft "near-balance" chime when progress crosses 80%.
- Show a transient "New best time!" badge on the win modal.

### 3.5 Discoverability
- Surface the Themes, Difficulty, Daily, and Achievements sections more obviously on Home (currently themes are a thin horizontal scroll).
- Add a "?" affordance on the Game HUD that reopens the tutorial.

### 3.6 User retention (summary; full plan in `PRODUCT_IMPROVEMENT_ROADMAP.md`)
- Streaks (exists — fix the year-boundary bug), achievements, daily challenge, gentle reminders.

### 3.7 UX roadmap (sequenced)
1. **Now:** fix dialog nesting (CRIT-4); rewrite tutorial copy; add semantics.
2. **Next:** redesign Settings into one screen; add Share result; honest leaderboard label.
3. **Later:** tablet/landscape layouts; achievements UI; onboarding animation.

---

## 4. Engagement & Retention Principles (UX guardrails)

- **No dark patterns:** no fake countdowns, no "your streak is in danger!" pressure, no pre-checked consent.
- **Honesty:** relabel simulated leaderboard; never imply real-time multiplayer that doesn't exist.
- **Optional, not naggy:** notifications and reminders are off by default and configurable.
- **Value-first:** every engagement mechanic must give the user something (a clearer goal, a reward, recognition) — not just extract a session.
