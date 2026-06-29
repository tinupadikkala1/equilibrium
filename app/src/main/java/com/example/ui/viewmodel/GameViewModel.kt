package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.platform.AdController
import com.example.platform.HapticController
import com.example.data.database.ActiveGameSave
import com.example.data.database.LevelProgress
import com.example.data.database.UserStats
import com.example.data.database.DailyChallengeProgress
import com.example.data.database.LevelAttemptHistory
import com.example.data.engine.LevelGenerator
import com.example.data.engine.GameSolver
import com.example.data.engine.GameRules
import com.example.data.repository.GameRepository
import com.example.sound.SoundEffectPlayer
import com.example.ui.viewmodel.collaborators.GameEngineState
import com.example.ui.viewmodel.collaborators.TimerController
import com.example.ui.viewmodel.collaborators.SaveManager
import com.example.ui.viewmodel.collaborators.EconomyController
import com.example.ui.viewmodel.collaborators.deepCopy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
    private val adController: AdController,
    private val hapticController: HapticController
) : ViewModel() {

    enum class Difficulty { ZEN, MASTER }

    // Collaborators
    private val engineState = GameEngineState()
    private val timerController = TimerController(viewModelScope)
    private val economyController = EconomyController(repository, viewModelScope)

    // Gameplay States delegated to collaborators
    val levelId get() = engineState.levelId
    val gridSize get() = engineState.gridSize
    val par get() = engineState.par
    val movesCount get() = engineState.movesCount
    val boardState get() = engineState.boardState
    val winState get() = engineState.winState
    val starredScore get() = engineState.starredScore
    val highlightedHintCell get() = engineState.highlightedHintCell
    val hintsUsedThisLevel get() = engineState.hintsUsedThisLevel

    val secondsElapsed get() = timerController.secondsElapsed
    val countdownSecondsLeft get() = timerController.countdownSecondsLeft
    val isTimeUp get() = timerController.isTimeUp

    // General states kept in ViewModel
    val gameDifficulty = MutableStateFlow(Difficulty.ZEN)
    val showToastMessage = MutableStateFlow<String?>(null)
    val isDailyChallenge = MutableStateFlow(false)
    val dailyDateKey = MutableStateFlow(0)
    val activeGameSave = MutableStateFlow<ActiveGameSave?>(null)

    fun clearToastMessage() {
        showToastMessage.value = null
    }

    private fun startTimer() {
        timerController.startTimer(
            difficulty = gameDifficulty.value,
            par = par.value,
            winStateFlow = winState,
            onTimeUp = { /* Time-up callback if needed */ }
        )
    }

    fun startGameWithDifficulty(difficulty: Difficulty) {
        gameDifficulty.value = difficulty
        startTimer()
        writeActiveGameSave(immediate = true)
    }

    // Room Persistent UI States
    val userStats: StateFlow<UserStats?> = repository.userStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allProgress: StateFlow<List<LevelProgress>> = repository.allLevelProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDailyProgress: StateFlow<List<DailyChallengeProgress>> = repository.allDailyProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun checkActiveGameSave() {
        viewModelScope.launch {
            activeGameSave.value = repository.getActiveGameSave()
        }
    }

    private var saveJob: Job? = null

    fun writeActiveGameSave(immediate: Boolean = false) {
        val currentBoard = boardState.value ?: return
        val currentInitialSnapshot = engineState.initialBoardSnapshot ?: return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            if (!immediate) delay(1500L)
            val save = ActiveGameSave(
                levelId = levelId.value,
                gridSize = gridSize.value,
                par = par.value,
                movesCount = movesCount.value,
                boardStateString = SaveManager.serializeBoard(currentBoard),
                initialBoardSnapshotString = SaveManager.serializeBoard(currentInitialSnapshot),
                secondsElapsed = secondsElapsed.value,
                isDailyChallenge = isDailyChallenge.value,
                dailyDateKey = dailyDateKey.value,
                gameDifficulty = gameDifficulty.value.name,
                historyStackString = SaveManager.serializeHistory(engineState.historyStack)
            )
            repository.saveActiveGame(save)
            activeGameSave.value = save
        }
    }

    fun deleteActiveGameSave() {
        saveJob?.cancel()
        viewModelScope.launch {
            repository.deleteActiveGameSave()
            activeGameSave.value = null
        }
    }

    fun resumeActiveGame() {
        viewModelScope.launch {
            val save = repository.getActiveGameSave() ?: return@launch
            isDailyChallenge.value = save.isDailyChallenge
            dailyDateKey.value = save.dailyDateKey
            levelId.value = save.levelId
            gridSize.value = save.gridSize
            par.value = save.par
            movesCount.value = save.movesCount
            winState.value = false
            starredScore.value = 0
            highlightedHintCell.value = null
            
            val bState = SaveManager.deserializeBoard(save.boardStateString)
            val initSnap = SaveManager.deserializeBoard(save.initialBoardSnapshotString)
            val parsedHistory = SaveManager.deserializeHistory(save.historyStackString)

            if (bState == null || initSnap == null || parsedHistory == null) {
                repository.deleteActiveGameSave()
                activeGameSave.value = null
                showToastMessage.value = "We couldn't resume your last session; starting fresh."
                loadLevel(1, startWithCurrentDifficulty = false)
                return@launch
            }

            boardState.value = bState
            engineState.initialBoardSnapshot = initSnap
            engineState.historyStack.clear()
            parsedHistory.forEach { engineState.historyStack.push(it) }
            
            // Restart timer with correct elapsed seconds
            timerController.resumeTimer(
                difficulty = if (save.gameDifficulty == "MASTER") Difficulty.MASTER else Difficulty.ZEN,
                par = par.value,
                secondsElapsedValue = save.secondsElapsed,
                winStateFlow = winState,
                onTimeUp = { /* Time-up callback if needed */ }
            )
            gameDifficulty.value = if (save.gameDifficulty == "MASTER") Difficulty.MASTER else Difficulty.ZEN
        }
    }

    init {
        viewModelScope.launch {
            repository.checkDailyStreak()
            repository.getOrCreateUserStats()
            checkActiveGameSave()
            loadLevel(1)
        }
    }

    fun loadLevel(id: Int, startWithCurrentDifficulty: Boolean = false) {
        viewModelScope.launch {
            isDailyChallenge.value = false
            levelId.value = id
            val generated = LevelGenerator.generateLevel(id)
            gridSize.value = generated.size
            par.value = generated.par
            engineState.reset()

            // Save deep copy of the board state
            val boardCopy = generated.board.deepCopy()
            boardState.value = boardCopy
            engineState.initialBoardSnapshot = generated.board.deepCopy()

            deleteActiveGameSave()

            if (startWithCurrentDifficulty) {
                startTimer()
            }
        }
    }

    fun loadNextLevel(startWithCurrentDifficulty: Boolean = true) {
        loadLevel(levelId.value + 1, startWithCurrentDifficulty)
    }

    fun loadDailyChallenge(dateKey: Int, startWithCurrentDifficulty: Boolean = false) {
        viewModelScope.launch {
            isDailyChallenge.value = true
            dailyDateKey.value = dateKey
            levelId.value = dateKey // level ID is represented by dateKey for daily
            val generated = LevelGenerator.generateDailyLevel(dateKey)
            gridSize.value = generated.size
            par.value = generated.par
            engineState.reset()

            // Save deep copy of the board state
            val boardCopy = generated.board.deepCopy()
            boardState.value = boardCopy
            engineState.initialBoardSnapshot = generated.board.deepCopy()

            deleteActiveGameSave()

            if (startWithCurrentDifficulty) {
                startTimer()
            }
        }
    }

    fun tapCell(r: Int, c: Int, context: Context) {
        val currentBoard = boardState.value ?: return
        if (winState.value) return

        // Push current copy to history stack
        engineState.historyStack.push(currentBoard.deepCopy())

        // Apply visual and auditory feedback
        playInteractionEffects(context)

        // Game play transformation formulas
        GameRules.applyTap(currentBoard, r, c)

        boardState.value = currentBoard.deepCopy() // emit update
        movesCount.value += 1

        // Clear active hints if user tapped somewhere
        highlightedHintCell.value = null

        writeActiveGameSave()

        checkWinCondition()
    }

    private fun checkWinCondition() {
        val currentBoard = boardState.value ?: return
        val won = GameRules.checkWinCondition(currentBoard)

        if (won) {
            winState.value = true
            timerController.stopTimer()
            val finalMoves = movesCount.value
            val parScore = par.value
            val starsEarned = GameRules.calculateStars(finalMoves, parScore)
            starredScore.value = starsEarned

            // Play celebratory win effects
            if (isSoundEnabled()) SoundEffectPlayer.playWin()

            deleteActiveGameSave()

            // Persist progression details to Room
            val timeTaken = secondsElapsed.value
            viewModelScope.launch {
                if (isDailyChallenge.value) {
                    repository.saveDailyProgress(dailyDateKey.value, true, finalMoves, timeTaken, starsEarned)
                } else {
                    repository.saveLevelProgress(levelId.value, starsEarned, finalMoves, timeTaken)
                }
            }
        }
    }

    fun undoMove() {
        if (engineState.historyStack.isEmpty()) return
        
        economyController.consumeUndo(
            onSuccess = {
                val previousBoard = engineState.historyStack.pop()
                boardState.value = previousBoard.deepCopy()
                movesCount.value = maxOf(0, movesCount.value - 1)
                highlightedHintCell.value = null
                writeActiveGameSave()
            },
            onFailure = { /* Handle error / show message */ }
        )
    }

    fun resetLevel() {
        val snapshot = engineState.initialBoardSnapshot ?: return
        boardState.value = snapshot.deepCopy()
        engineState.reset()
        writeActiveGameSave()
    }

    // Zen hint state
    val zenHintsRemaining = MutableStateFlow(10)
    val zenHintAdRequired = MutableStateFlow(false)

    fun triggerHint() {
        val currentBoard = boardState.value ?: return
        val isZen = gameDifficulty.value == Difficulty.ZEN
        if (!isZen && hintsUsedThisLevel.value >= 2) return

        val getHintAction = {
            hintsUsedThisLevel.value += 1
            val bestCell = GameSolver.getBestHintCell(currentBoard, gridSize.value)
            if (bestCell != null) {
                highlightedHintCell.value = bestCell
                viewModelScope.launch {
                    delay(4000)
                    if (highlightedHintCell.value == bestCell) {
                        highlightedHintCell.value = null
                    }
                }
            }
        }

        if (isZen) {
            // Zen mode: hints are always free, no limit
            getHintAction()
        } else {
            economyController.consumeHint(
                onSuccess = getHintAction,
                onFailure = { /* No hints left — UI should show ad option */ }
            )
        }
    }

    fun onZenHintAdWatched() {
        viewModelScope.launch {
            repository.addHints(2)
            zenHintAdRequired.value = false
            // Grant 2 hints via the normal hint economy for use
            // Trigger the hint automatically after ad
            val currentBoard = boardState.value ?: return@launch
            hintsUsedThisLevel.value += 1
            val bestCell = GameSolver.getBestHintCell(currentBoard, gridSize.value)
            if (bestCell != null) {
                highlightedHintCell.value = bestCell
                delay(4000)
                if (highlightedHintCell.value == bestCell) {
                    highlightedHintCell.value = null
                }
            }
        }
    }

    fun earnReward(type: String, amount: Int) {
        if (type == "time") {
            timerController.addSeconds(amount)
            return
        }
        economyController.grantReward(type, amount)
    }

    fun skipLevel() {
        economyController.consumeSkip(
            onSuccess = {
                viewModelScope.launch {
                    repository.saveLevelProgress(levelId.value, 1, par.value, 0)
                    delay(200)
                    loadLevel(levelId.value + 1)
                }
            },
            onFailure = { /* Handle failure */ }
        )
    }

    fun switchTheme(themeName: String) {
        viewModelScope.launch {
            repository.updateTheme(themeName)
        }
    }

    fun toggleSoundValue(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSound(enabled)
        }
    }

    fun toggleMusicValue(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleMusic(enabled)
        }
    }

    fun toggleHapticValue(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleHaptic(enabled)
        }
    }

    fun resetAllGameData() {
        viewModelScope.launch {
            repository.clearAllProgressAndResetStats()
            loadLevel(1)
        }
    }

    private fun isSoundEnabled(): Boolean {
        return userStats.value?.soundEnabled ?: true
    }

    private fun isHapticEnabled(): Boolean {
        return userStats.value?.hapticEnabled ?: true
    }

    private fun playInteractionEffects(context: Context) {
        if (isSoundEnabled()) {
            SoundEffectPlayer.playTap()
        }
        if (isHapticEnabled()) {
            hapticController.vibrate(context, 30)
        }
    }

    class Factory(
        private val repository: GameRepository,
        private val adController: AdController,
        private val hapticController: HapticController
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository, adController, hapticController) as T
        }
    }

    fun getHistoryForLevel(levelId: Int, isDaily: Boolean): Flow<List<LevelAttemptHistory>> {
        return repository.getHistoryForLevel(levelId, isDaily)
    }

    fun getAllHistory(): Flow<List<LevelAttemptHistory>> {
        return repository.getAllHistory()
    }

    override fun onCleared() {
        if (!winState.value && boardState.value != null) {
            saveJob?.cancel()
            val board = boardState.value ?: return
            val snapshot = engineState.initialBoardSnapshot ?: return
            kotlinx.coroutines.runBlocking {
                repository.saveActiveGame(
                    ActiveGameSave(
                        levelId = levelId.value, gridSize = gridSize.value, par = par.value,
                        movesCount = movesCount.value,
                        boardStateString = SaveManager.serializeBoard(board),
                        initialBoardSnapshotString = SaveManager.serializeBoard(snapshot),
                        secondsElapsed = secondsElapsed.value,
                        isDailyChallenge = isDailyChallenge.value,
                        dailyDateKey = dailyDateKey.value,
                        gameDifficulty = gameDifficulty.value.name,
                        historyStackString = SaveManager.serializeHistory(engineState.historyStack)
                    )
                )
            }
        }
        super.onCleared()
    }
}
