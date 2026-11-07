package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.database.UserStats
import com.example.data.engine.LevelGenerator
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.EquilibriumTheme
import com.example.ui.theme.VisualTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real Android view hierarchy without changing the player's database. */
@RunWith(AndroidJUnit4::class)
class ThemeDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bothThemesExposePlayableControlsAndRewardRecovery() {
        var theme by mutableStateOf(VisualTheme.ORBITAL.displayName)
        var scene by mutableStateOf("home")
        var hints by mutableIntStateOf(0)
        var rewards = 0
        var hintUses = 0
        var restartUses = 0
        compose.setContent {
            EquilibriumTheme(theme) {
                val stats = UserStats(themeName = theme, tutorialCompleted = true, hints = hints)
                if (scene == "home") {
                    HomeScreen(stats, emptyList(), null, emptyList(), { scene = "game" }, {}, { theme = it }, {}, {}, {}, {}, banner = {})
                } else {
                    GameScreen(
                        levelId = 1, par = 4, movesCount = 1, board = LevelGenerator.generateLevel(1).board,
                        winState = false, starredScore = 0, highlightedCell = null, userStats = stats,
                        secondsElapsed = 5, bestTimeSeconds = 0, previousBestMoves = 0,
                        countdownSecondsLeft = 75, isTimeUp = false, canUndo = true, busy = false,
                        onCellTapped = { _, _ -> }, onUndo = {}, onReset = { restartUses++ },
                        onHint = { hintUses++; hints-- }, onSkip = {}, onNextLevel = {}, onBack = { scene = "home" },
                        onToggleSound = {}, onToggleMusic = {}, onToggleHaptic = {}, onSwitchTheme = { theme = it },
                        onTutorialComplete = {}, onOverlayChanged = {},
                        onReward = { _, closed -> rewards++; hints += 3; closed() }, onInterstitial = { it() }, banner = {},
                    )
                }
            }
        }
        for (mode in VisualTheme.entries) {
            compose.onNodeWithTag("home_settings").performClick()
            compose.onNodeWithTag("theme_${mode.name.lowercase()}").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("settings_done").performScrollTo().performClick()
            compose.onNodeWithText("Let's play").performClick()
            compose.onNodeWithTag("game_screen").assertExists()
            compose.onNodeWithTag("hint_button").performScrollTo()
            compose.onNodeWithText("Hint").performClick()
            compose.onNodeWithTag("reward_dialog").assertExists()
            compose.onNodeWithText("Not now").performClick()
            compose.onNodeWithText("Hint").performClick()
            compose.onNodeWithText("Watch & earn 3 hints").performClick()
            compose.onNodeWithTag("reward_dialog").assertDoesNotExist()
            compose.onNodeWithText("Hint").performClick()
            compose.onNodeWithText("2 left").assertExists()
            compose.onNodeWithText("Restart").performClick()
            compose.onNodeWithText("Restart stage").performClick()
            compose.onNodeWithTag("restart_dialog").assertDoesNotExist()
            compose.onNodeWithTag("settings_menu_button").performScrollTo().performClick()
            compose.onNodeWithText("Save & leave").performScrollTo().performClick()
            compose.onNodeWithTag("home_screen").assertExists()
            compose.runOnIdle { hints = 0 }
        }
        compose.runOnIdle {
            assertEquals(2, rewards)
            assertEquals(2, hintUses)
            assertEquals(2, restartUses)
        }
    }

    @Test fun bothThemesSupportLargeTextOnDevice() {
        var theme by mutableStateOf(VisualTheme.ORBITAL.displayName)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                EquilibriumTheme(theme) { SettingsDialog(UserStats(themeName = theme), { theme = it }, {}, {}, {}, {}, {}, {}) }
            }
        }
        for (mode in VisualTheme.entries) {
            compose.onNodeWithTag("theme_${mode.name.lowercase()}").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("settings_done").performScrollTo().assertIsDisplayed()
        }
    }
}
