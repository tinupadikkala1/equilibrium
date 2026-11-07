package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.*
import com.example.platform.AdController
import com.example.sound.AmbientMusicPlayer
import com.example.sound.SoundEffectPlayer
import com.example.ui.components.GameBackdrop
import com.example.ui.components.BalanceEmblem
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModel()
    private val adController: AdController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adController.initialize(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    viewModel.userStats.collect { stats ->
                        if (stats?.musicEnabled == true) AmbientMusicPlayer.start() else AmbientMusicPlayer.stop()
                    }
                } finally { AmbientMusicPlayer.stop() }
            }
        }
        setContent {
            val stats by viewModel.userStats.collectAsStateWithLifecycle()
            val loaded by viewModel.loaded.collectAsStateWithLifecycle()
            val progress by viewModel.allProgress.collectAsStateWithLifecycle()
            val save by viewModel.activeGameSave.collectAsStateWithLifecycle()
            val history by viewModel.allHistory.collectAsStateWithLifecycle()
            val message by viewModel.showToastMessage.collectAsStateWithLifecycle()
            val theme = VisualTheme.fromName(stats?.themeName ?: VisualTheme.ORBITAL.displayName)
            LaunchedEffect(theme) {
                val style = if (theme == VisualTheme.ORBITAL) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LaunchedEffect(message) {
                message?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show(); viewModel.clearToastMessage() }
            }
            EquilibriumTheme(theme.displayName) {
                if (!loaded || stats == null) {
                    GameBackdrop(Modifier.fillMaxSize()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            BalanceEmblem()
                            Spacer(Modifier.height(20.dp))
                            Text("Equilibrium", color = LocalGamePalette.current.ink, style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(24.dp))
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                } else {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                stats, progress, save, history,
                                onSelectLevel = { viewModel.loadLevel(it); nav.navigate("game") { launchSingleTop = true } },
                                onResumeGame = { viewModel.resumeActiveGame(); nav.navigate("game") { launchSingleTop = true } },
                                onSwitchTheme = viewModel::switchTheme,
                                onToggleSound = viewModel::toggleSoundValue, onToggleMusic = viewModel::toggleMusicValue,
                                onToggleHaptic = viewModel::toggleHapticValue, onResetAllData = viewModel::resetAllGameData,
                            )
                        }
                        composable("game") {
                            DisposableEffect(Unit) {
                                viewModel.setGameVisible(true)
                                onDispose { viewModel.setGameVisible(false) }
                            }
                            val level by viewModel.levelId.collectAsStateWithLifecycle()
                            val par by viewModel.par.collectAsStateWithLifecycle()
                            val moves by viewModel.movesCount.collectAsStateWithLifecycle()
                            val board by viewModel.boardState.collectAsStateWithLifecycle()
                            val won by viewModel.winState.collectAsStateWithLifecycle()
                            val stars by viewModel.starredScore.collectAsStateWithLifecycle()
                            val hint by viewModel.highlightedHintCell.collectAsStateWithLifecycle()
                            val elapsed by viewModel.secondsElapsed.collectAsStateWithLifecycle()
                            val remaining by viewModel.countdownSecondsLeft.collectAsStateWithLifecycle()
                            val timeUp by viewModel.isTimeUp.collectAsStateWithLifecycle()
                            val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
                            val busy by viewModel.busy.collectAsStateWithLifecycle()
                            val bestTime by viewModel.bestTimeSeconds.collectAsStateWithLifecycle()
                            val bestMoves by viewModel.previousBestMoves.collectAsStateWithLifecycle()
                            LaunchedEffect(board) {
                                // Restore a navigation back stack after process death.
                                if (board == null && save != null) viewModel.resumeActiveGame()
                                else if (board == null) nav.popBackStack()
                            }
                            GameScreen(
                                levelId = level, par = par, movesCount = moves, board = board,
                                winState = won, starredScore = stars, highlightedCell = hint, userStats = stats,
                                secondsElapsed = elapsed, bestTimeSeconds = bestTime, previousBestMoves = bestMoves,
                                countdownSecondsLeft = remaining, isTimeUp = timeUp, canUndo = canUndo, busy = busy,
                                onCellTapped = { r, c -> viewModel.tapCell(r, c, this@MainActivity) },
                                onUndo = viewModel::undoMove, onReset = viewModel::resetLevel, onHint = viewModel::triggerHint,
                                onSkip = viewModel::skipLevel, onNextLevel = viewModel::loadNextLevel,
                                onBack = { viewModel.saveNow(); nav.popBackStack() },
                                onToggleSound = viewModel::toggleSoundValue, onToggleMusic = viewModel::toggleMusicValue,
                                onToggleHaptic = viewModel::toggleHapticValue, onSwitchTheme = viewModel::switchTheme,
                                onTutorialComplete = viewModel::completeTutorial, onOverlayChanged = viewModel::setOverlayOpen,
                                onReward = { type, closed ->
                                    var earned = false
                                    adController.showRewarded(this@MainActivity, { amount ->
                                        earned = true
                                        viewModel.earnReward(type, amount)
                                    }, {
                                        if (!earned) viewModel.notify("No reward earned. The ad may be unavailable—please try again.")
                                        closed()
                                    })
                                },
                                onInterstitial = { closed -> adController.showInterstitial(this@MainActivity, closed) },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); viewModel.setForeground(true) }
    override fun onPause() { viewModel.setForeground(false); AmbientMusicPlayer.stop(); super.onPause() }
    override fun onDestroy() { SoundEffectPlayer.release(); super.onDestroy() }
}
