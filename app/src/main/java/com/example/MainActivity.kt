package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.GameRepository
import com.example.sound.AmbientMusicPlayer
import com.example.sound.SoundEffectPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.DifficultySelectScreen
import com.example.ui.theme.EquilibriumTheme
import com.example.platform.AdController
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModel()
    private val adController: AdController by inject()
    private val repository: GameRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adController.initialize(this)

        setContent {
            // Observe live Room database stats
            val userStats by viewModel.userStats.collectAsState()
            val levelProgressList by viewModel.allProgress.collectAsState()
            val dailyProgressList by viewModel.allDailyProgress.collectAsState()
            val activeGameSave by viewModel.activeGameSave.collectAsState()
            val allHistory by viewModel.getAllHistory().collectAsState(initial = emptyList())

            LaunchedEffect(userStats?.musicEnabled) {
                if (userStats?.musicEnabled != false) {
                    AmbientMusicPlayer.start()
                } else {
                    AmbientMusicPlayer.stop()
                }
            }

            val toastMessage by viewModel.showToastMessage.collectAsState()
            LaunchedEffect(toastMessage) {
                toastMessage?.let {
                    android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.clearToastMessage()
                }
            }

            EquilibriumTheme(
                themeName = userStats?.themeName ?: "Neon Pulse"
            ) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    // Home Level Select Screen Stage
                    composable("home") {
                        HomeScreen(
                            userStats = userStats,
                            levelProgressList = levelProgressList,
                            dailyProgressList = dailyProgressList,
                            activeGameSave = activeGameSave,
                            allHistory = allHistory,
                            onSelectLevel = { levelNum ->
                                viewModel.loadLevel(levelNum, startWithCurrentDifficulty = false)
                                navController.navigate("difficulty_select")
                            },
                            onSelectDailyLevel = { dateKey ->
                                viewModel.loadDailyChallenge(dateKey, startWithCurrentDifficulty = false)
                                navController.navigate("difficulty_select")
                            },
                            onResumeGame = {
                                viewModel.resumeActiveGame()
                                navController.navigate("game")
                            },
                            onSwitchTheme = { t ->
                                viewModel.switchTheme(t)
                            },
                            onToggleSound = { enabled ->
                                viewModel.toggleSoundValue(enabled)
                            },
                            onToggleMusic = { enabled ->
                                viewModel.toggleMusicValue(enabled)
                            },
                            onToggleHaptic = { enabled ->
                                viewModel.toggleHapticValue(enabled)
                            },
                            onResetAllData = {
                                viewModel.resetAllGameData()
                            }
                        )
                    }

                    // Difficulty Selection Screen Route
                    composable("difficulty_select") {
                        val levelId by viewModel.levelId.collectAsState()
                        val isDailyChallenge by viewModel.isDailyChallenge.collectAsState()

                        DifficultySelectScreen(
                            levelId = levelId,
                            isDailyChallenge = isDailyChallenge,
                            onSelectDifficulty = { difficulty ->
                                viewModel.startGameWithDifficulty(difficulty)
                                navController.navigate("game") {
                                    popUpTo("home") { saveState = false }
                                }
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Active Puzzle Game Screen Stage
                    composable("game") {
                        val levelId by viewModel.levelId.collectAsState()
                        val gridSize by viewModel.gridSize.collectAsState()
                        val par by viewModel.par.collectAsState()
                        val movesCount by viewModel.movesCount.collectAsState()
                        val board by viewModel.boardState.collectAsState()
                        val winState by viewModel.winState.collectAsState()
                        val starredScore by viewModel.starredScore.collectAsState()
                        val highlightedCell by viewModel.highlightedHintCell.collectAsState()
                        val secondsElapsed by viewModel.secondsElapsed.collectAsState()
                        val hintsUsedThisLevel by viewModel.hintsUsedThisLevel.collectAsState()
                        val isDailyChallenge by viewModel.isDailyChallenge.collectAsState()
                        val dailyDateKey by viewModel.dailyDateKey.collectAsState()
                        val dailyProgressList by viewModel.allDailyProgress.collectAsState()
                        val bestTimeSeconds = if (isDailyChallenge) {
                            dailyProgressList.find { it.dateKey == dailyDateKey }?.timeTakenSeconds ?: 0
                        } else {
                            levelProgressList.find { it.levelId == levelId }?.bestTimeSeconds ?: 0
                        }
                        val previousBestMoves = if (isDailyChallenge) {
                            dailyProgressList.find { it.dateKey == dailyDateKey }?.movesTaken ?: 0
                        } else {
                            levelProgressList.find { it.levelId == levelId }?.bestMoves ?: 0
                        }

                        val difficultyMode by viewModel.gameDifficulty.collectAsState()
                        val countdownSecondsLeft by viewModel.countdownSecondsLeft.collectAsState()
                        val isTimeUp by viewModel.isTimeUp.collectAsState()

                        val onCellTapped = remember(viewModel) { { r: Int, c: Int -> viewModel.tapCell(r, c, this@MainActivity) } }
                        val onUndo = remember(viewModel) { { viewModel.undoMove() } }
                        val onReset = remember(viewModel) { { viewModel.resetLevel() } }
                        val onHint = remember(viewModel) { { viewModel.triggerHint() } }
                        val onSkip = remember(viewModel) { { viewModel.skipLevel() } }
                        val onToggleSound = remember(viewModel) { { enabled: Boolean -> viewModel.toggleSoundValue(enabled) } }
                        val onToggleHaptic = remember(viewModel) { { enabled: Boolean -> viewModel.toggleHapticValue(enabled) } }
                        val onNextLevel = remember(viewModel) { { viewModel.loadNextLevel() } }
                        val onChangeDifficulty = remember(viewModel) { { diff: GameViewModel.Difficulty -> viewModel.startGameWithDifficulty(diff) } }
                        val onEarnReward = remember(viewModel) { { type: String, amount: Int -> viewModel.earnReward(type, amount) } }
                        val onShowRewardedAd = remember(adController) { { type: String, onRewardEarned: (Int) -> Unit -> adController.showRewarded(this@MainActivity, onRewardEarned, {}) } }
                        val onShowInterstitialAd = remember(adController) { { onClosed: () -> Unit -> adController.showInterstitial(this@MainActivity, onClosed) } }
                        val onBack = remember(navController) { { navController.popBackStack(); Unit } }

                        GameScreen(
                            levelId = levelId,
                            gridSize = gridSize,
                            par = par,
                            movesCount = movesCount,
                            board = board,
                            winState = winState,
                            starredScore = starredScore,
                            highlightedCell = highlightedCell,
                            userStats = userStats,
                            secondsElapsed = secondsElapsed,
                            hintsUsedThisLevel = hintsUsedThisLevel,
                            bestTimeSeconds = bestTimeSeconds,
                            previousBestMoves = previousBestMoves,
                            isDailyChallenge = isDailyChallenge,
                            difficultyMode = difficultyMode,
                            countdownSecondsLeft = countdownSecondsLeft,
                            isTimeUp = isTimeUp,
                            onCellTapped = onCellTapped,
                            onUndo = onUndo,
                            onReset = onReset,
                            onHint = onHint,
                            onSkip = onSkip,
                            onToggleSound = onToggleSound,
                            onToggleHaptic = onToggleHaptic,
                            onNextLevel = onNextLevel,
                            onChangeDifficulty = onChangeDifficulty,
                            onEarnReward = onEarnReward,
                            onShowRewardedAd = onShowRewardedAd,
                            onShowInterstitialAd = onShowInterstitialAd,
                            onBack = onBack
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AmbientMusicPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundEffectPlayer.release()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val stats = repository.getOrCreateUserStats()
            if (stats.musicEnabled) {
                AmbientMusicPlayer.start()
            }
        }
    }
}
