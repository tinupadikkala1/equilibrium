# Equilibrium

An Android number-balancing puzzle built with Kotlin, Jetpack Compose, Room and Koin.

Tap a tile to subtract one from it and add one to each orthogonal neighbor.
Make all values equal before the countdown expires. All stages use the Master
countdown; there is no Zen selector or Daily Challenge screen.

## Appearance

Choose a persistent appearance in Settings or the pause menu:

- **Orbital Glass:** dark navy, luminous mint and violet, layered panels.
- **Pop Machine:** light lavender, coral and gold, bold outlines and raised controls.

Both modes cover Home, stage selection, gameplay, settings, practice/demo, history,
pause, restart/skip confirmation, victory, time-up and optional reward dialogs.
The original stage progression and banner/interstitial/rewarded ad integrations remain.

## Build

Open this directory in Android Studio with Android SDK 36 installed. The Gradle
wrapper uses Java 17 in the verified local environment. No Gemini API key is
needed by the game.

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Local installs use the existing ignored `debug.keystore` when present, otherwise
the Android user's standard `~/.android/debug.keystore`. Do not commit signing
keys, passwords or `.env`.

Release signing uses `KEYSTORE_PATH` (default `my-upload-key.jks`),
`STORE_PASSWORD`, `KEY_PASSWORD`, and key alias `upload`. Release ad IDs are
read from the `ADMOB_APP_ID`, `BANNER_AD_UNIT_ID`, `INTERSTITIAL_AD_UNIT_ID`,
and `REWARDED_AD_UNIT_ID` Gradle properties or environment variables. The existing
configuration falls back to Google's test IDs; configure production values before
publishing.

## Verify

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:recordRoborazziDebug --tests com.example.ThemeScreensTest
./gradlew :app:compileReleaseKotlin :app:minifyReleaseWithR8 :app:lintRelease
```

The unit suite covers deterministic solvable stages, timers, save validation,
session recovery, currency updates, Room migrations, theme contrast, Compose
navigation/dialog interactions and large text. Device tests exercise both themes
and reward/restart flows with controlled callbacks, without clearing player data.

Reports live in `app/build/reports/`; rendered screenshots are in
`app/build/reports/theme-screenshots/`. See `docs/UI_VERIFICATION.md` for results
and remaining validation boundaries.

Database version 8 migrates versions 6 and 7 without destructive fallback. Legacy
daily records stay archived; campaign saves become timed saves. The current schema
is versioned in `app/schemas/`.

The older audit and roadmap documents describe historical project states. This
README and the current source describe the implemented stage-only game.
