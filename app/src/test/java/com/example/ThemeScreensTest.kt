package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.database.UserStats
import com.example.data.engine.LevelGenerator
import com.example.ui.components.GameBackdrop
import com.example.ui.screens.*
import com.example.ui.screens.game.*
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h760dp-mdpi", application = android.app.Application::class)
class ThemeScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun orbitalScreensAndControls() = gallery(VisualTheme.ORBITAL)
    @Test fun popScreensAndControls() = gallery(VisualTheme.POP)

    @Test
    @Config(sdk = [24, 36])
    fun tutorialRemainsPlayableOnOldestAndTargetAndroid() {
        var theme by mutableStateOf(VisualTheme.ORBITAL.displayName)
        compose.setContent {
            EquilibriumTheme(theme) { TutorialOverlay {} }
        }
        for (mode in VisualTheme.entries) {
            compose.runOnIdle { theme = mode.displayName }
            compose.onNodeWithTag("tutorial_overlay").assertExists()
            compose.onNodeWithTag("tutorial_finish").performScrollTo().assertExists()
        }
        compose.onNodeWithTag("tile_1_1").performScrollTo().performClick()
        compose.onNodeWithText("That's equilibrium.").assertExists()
    }

    @Test fun largeTextKeepsBothThemeControlsReachable() {
        var selectedTheme by mutableStateOf(VisualTheme.ORBITAL.displayName)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                EquilibriumTheme(selectedTheme) {
                    SettingsDialog(UserStats(themeName = selectedTheme), { selectedTheme = it }, {}, {}, {}, {}, {}, {})
                }
            }
        }
        for (theme in VisualTheme.entries) {
            compose.onNodeWithTag("theme_${theme.name.lowercase()}").performScrollTo().performClick()
            compose.onNodeWithTag("theme_${theme.name.lowercase()}").assertIsSelected()
            compose.onNodeWithTag("settings_done").performScrollTo().assertExists()
        }
    }

    private fun gallery(theme: VisualTheme) {
        var scene by mutableStateOf("home")
        var selectedTheme by mutableStateOf(theme.displayName)
        var stats by mutableStateOf(UserStats(themeName = theme.displayName, tutorialCompleted = true))
        var hintInvocations = 0
        val board = LevelGenerator.generateLevel(1).board
        compose.setContent {
            EquilibriumTheme(selectedTheme) {
                when (scene) {
                    "home" -> HomeScreen(stats, emptyList(), null, emptyList(), {}, {}, { selectedTheme = it; stats = stats.copy(themeName = it) }, {}, {}, {}, {}, banner = {})
                    "game", "large-board" -> GameScreen(
                        levelId = 1, par = 4, movesCount = 1,
                        board = if (scene == "large-board") LevelGenerator.generateLevel(70).board else board,
                        winState = false, starredScore = 0, highlightedCell = null, userStats = stats,
                        secondsElapsed = 8, bestTimeSeconds = 0, previousBestMoves = 0, countdownSecondsLeft = 72,
                        isTimeUp = false, canUndo = true, busy = false,
                        onCellTapped = { _, _ -> }, onUndo = {}, onReset = {}, onHint = { hintInvocations++ },
                        onSkip = {}, onNextLevel = {}, onBack = {}, onToggleSound = {}, onToggleMusic = {},
                        onToggleHaptic = {}, onSwitchTheme = { selectedTheme = it; stats = stats.copy(themeName = it) },
                        onTutorialComplete = {}, onOverlayChanged = {}, onReward = { _, done -> done() }, onInterstitial = { it() }, banner = {},
                    )
                    else -> GameBackdrop(Modifier.fillMaxSize()) {
                        when (scene) {
                            "settings" -> SettingsDialog(stats, { selectedTheme = it }, {}, {}, {}, {}, {}, {})
                            "tutorial" -> TutorialOverlay {}
                            "demo" -> DemoDialog {}
                            "victory" -> WinDialog(3, 4, 4, 1, 5, 16, {}, {}, {}, {})
                            "time-up" -> TimeUpDialog({}, {}, {})
                            "reward" -> RewardDialog("hint", false, {}, {})
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("home_play").assertExists()
        compose.onNodeWithTag("home_settings").performClick()
        compose.onNodeWithTag("settings_dialog").assertExists()
        val other = if (theme == VisualTheme.ORBITAL) VisualTheme.POP else VisualTheme.ORBITAL
        compose.onNodeWithTag("theme_${other.name.lowercase()}").performScrollTo().performClick()
        compose.onNodeWithTag("theme_${other.name.lowercase()}").assertIsSelected()
        compose.onNodeWithTag("theme_${theme.name.lowercase()}").performScrollTo().performClick()
        compose.onNodeWithTag("settings_done").performScrollTo().performClick()
        compose.onNodeWithTag("home_screen").captureRoboImage("build/reports/theme-screenshots/${theme.name.lowercase()}-home.png")
        compose.runOnIdle { scene = "game" }
        compose.onNodeWithTag("hint_button").performScrollTo()
        compose.onNodeWithText("Hint").performClick()
        compose.runOnIdle { check(hintInvocations == 1) }
        compose.onNodeWithTag("game_screen").captureRoboImage("build/reports/theme-screenshots/${theme.name.lowercase()}-game.png")
        compose.onNodeWithTag("settings_menu_button").performScrollTo().performClick()
        compose.onNodeWithTag("pause_dialog").captureRoboImage("build/reports/theme-screenshots/${theme.name.lowercase()}-pause.png")
        compose.onNodeWithTag("pause_resume").performClick()
        compose.onNodeWithTag("reset_button").performScrollTo()
        compose.onNodeWithText("Restart", substring = false).performClick()
        compose.onNodeWithTag("restart_dialog").captureRoboImage("build/reports/theme-screenshots/${theme.name.lowercase()}-restart.png")
        compose.onNodeWithText("Keep playing").performClick()
        for (screen in listOf("large-board", "settings", "tutorial", "demo", "victory", "time-up", "reward")) {
            compose.runOnIdle { scene = screen }
            compose.waitForIdle()
            val tag = when (screen) {
                "large-board" -> "game_screen"
                "settings" -> "settings_dialog"
                "tutorial" -> "tutorial_overlay"
                "demo" -> "demo_dialog"
                "victory" -> "win_dialog"
                "time-up" -> "times_up_dialog"
                else -> "reward_dialog"
            }
            compose.onNodeWithTag(tag).assertExists().captureRoboImage("build/reports/theme-screenshots/${theme.name.lowercase()}-$screen.png")
            if (screen == "tutorial") {
                compose.onNodeWithTag("tile_1_1").performClick()
                compose.onNodeWithText("That's equilibrium.").assertExists()
            }
        }
    }
}
