# Orbital Glass / Pop Machine verification

Verified locally on November 7, 2026. The implementation starts from commit
`7f5b037`, completes Zen/Daily Challenge removal, and uses the Master countdown
for all stages.

## Completed checks

- Full Java 21 unit suite: **24 tests passed, zero failures or skipped tests**, including
  the tutorial on API 24 and API 36 and the main Compose suite on API 34.
- Debug APK compilation, packaging and installation.
- Release Kotlin compilation and R8 optimization (`minifyReleaseWithR8`).
- Debug and release lint: **0 errors, 45 warnings**. Remaining warnings concern
  dependency versions and existing resources/launcher icons; they are not suppressed.
- Three instrumented tests passed on the connected Realme RMX2030, Android 10
  (API 29): package identity, both themes' controls/reward/restart paths, and
  settings at 160% text size. Reward callbacks are controlled test doubles.
- Unit/integration checks cover 1,000 deterministic solvable stages, board/history
  parsing, countdown pause/resume/expiry, added time, undo costs, sequential reward
  credits, settings persistence, save/relaunch, reset, completed-stage input guards,
  corrupt-save recovery, and migrations from database versions 6 and 7 to 8.
- Compose screen tests cover home, 3x3 and 5x5 boards, pause, settings, tutorial,
  demo, victory, time-up, restart confirmation and reward dialogs in both modes.
- Normal/action text contrast checks pass for both palettes. Large-text settings
  controls remain reachable at 160% scale.
- Captured 22 rendered Compose screens with Roborazzi, plus installed-device
  screenshots. Generate the local comparison page with `python3 scripts/theme_gallery.py`.
- Manual device flow: theme switches, practice tap, countdown expiry, reward
  cancellation, retry, solving stage 1, three-star victory, next stage, pause,
  save/leave, process restart and resume. No fatal Android runtime exception was
  observed during these checks.
- `git diff --check` and Python helper syntax checks.

The device-test runner removes installed test packages during cleanup. The game
was reinstalled and its pre-test database backup restored, then migration and
startup were checked again. Test-earned progress is intentionally not retained.

## Runtime requirement found during testing

The extended API 36 Robolectric check initially could not create its sandbox on
Java 17. Unit-test tasks now explicitly select a Java 21 toolchain. This changes
the test JVM only; the app's bytecode target and minimum Android version remain
unchanged. The full rerun passed on Java 21. Install JDK 21 or enable Gradle
toolchain provisioning before running the full suite.

## Validation boundaries

- A signed production APK/AAB was not produced: release signing passwords are not
  configured. Release compilation and shrinking were checked independently.
- Real AdMob video delivery, fill rate and real-money monetization were not
  certified. The existing release configuration still defaults to Google test
  IDs until production properties are supplied.
- Screenshot files were captured, but this session's image-inspection tool could
  not display them. Automated interaction/bounds/contrast checks and device UI
  hierarchy inspection do not replace a human visual review.
- Testing is bounded; it cannot prove absence of every defect or cover every
  Android device, display size, locale, accessibility service or long-running
  session. No store publication or Play Console validation was performed.

## Reproduce

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:recordRoborazziDebug --tests com.example.ThemeScreensTest
python3 scripts/theme_gallery.py
./gradlew :app:compileReleaseKotlin :app:minifyReleaseWithR8 :app:lintRelease
```

Use a dedicated test device or back up game data before connected tests, because
the Android test runner may uninstall the package during cleanup.
