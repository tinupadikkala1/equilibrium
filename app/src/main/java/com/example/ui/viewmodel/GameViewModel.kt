package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ActiveGameSave
import com.example.data.engine.GameRules
import com.example.data.engine.GameSolver
import com.example.data.engine.LevelGenerator
import com.example.data.repository.GameRepository
import com.example.platform.AdController
import com.example.platform.HapticController
import com.example.sound.SoundEffectPlayer
import com.example.ui.theme.VisualTheme
import com.example.ui.viewmodel.collaborators.GameEngineState
import com.example.ui.viewmodel.collaborators.SaveManager
import com.example.ui.viewmodel.collaborators.TimerController
import com.example.ui.viewmodel.collaborators.deepCopy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameViewModel(
    private val repository: GameRepository,
    @Suppress("unused") private val adController: AdController,
    private val hapticController: HapticController
) : ViewModel() {
    private val engine = GameEngineState()
    private val timer = TimerController(viewModelScope)
    private val persistenceMutex = Mutex()
    private var saveJob: Job? = null
    private var hintJob: Job? = null
    private var session = 0
    private var foreground = false
    private var onGameScreen = false
    private var overlayOpen = false
    private var ready = false

    val levelId get() = engine.levelId
    val gridSize get() = engine.gridSize
    val par get() = engine.par
    val movesCount get() = engine.movesCount
    val boardState get() = engine.boardState
    val winState get() = engine.winState
    val starredScore get() = engine.starredScore
    val highlightedHintCell get() = engine.highlightedHintCell
    val secondsElapsed get() = timer.secondsElapsed
    val countdownSecondsLeft get() = timer.countdownSecondsLeft
    val isTimeUp get() = timer.isTimeUp
    val showToastMessage = MutableStateFlow<String?>(null)
    val activeGameSave = MutableStateFlow<ActiveGameSave?>(null)
    val loaded = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val canUndo = MutableStateFlow(false)
    val bestTimeSeconds = MutableStateFlow(0)
    val previousBestMoves = MutableStateFlow(0)
    val userStats = repository.userStatsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val allProgress = repository.allLevelProgress.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allHistory = repository.getAllHistory().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            repository.getOrCreateUserStats()
            repository.checkDailyStreak()
            activeGameSave.value = repository.getActiveGameSave()
            loaded.value = true
        }
    }

    fun clearToastMessage() { showToastMessage.value = null }
    fun notify(message: String) { showToastMessage.value = message }

    private fun updateClock() {
        if (foreground && onGameScreen && ready && !overlayOpen && !winState.value && !isTimeUp.value) timer.resume()
        else timer.pause()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        updateClock()
        if (!value) saveNow()
    }

    fun setGameVisible(value: Boolean) {
        onGameScreen = value
        updateClock()
        if (!value) saveNow()
    }

    fun setOverlayOpen(value: Boolean) { overlayOpen = value; updateClock() }

    fun loadLevel(id: Int) {
        session += 1
        hintJob?.cancel()
        timer.pause()
        val level = LevelGenerator.generateLevel(id.coerceAtLeast(1))
        engine.reset()
        levelId.value = level.levelId
        gridSize.value = level.size
        par.value = level.par
        boardState.value = level.board.deepCopy()
        engine.initialBoardSnapshot = level.board.deepCopy()
        val best = allProgress.value.find { it.levelId == id }
        previousBestMoves.value = best?.bestMoves ?: 0
        bestTimeSeconds.value = best?.bestTimeSeconds ?: 0
        canUndo.value = false
        timer.reset(level.par)
        ready = true
        saveNow()
        updateClock()
    }

    fun loadNextLevel() = loadLevel(levelId.value + 1)

    fun resumeActiveGame() {
        val save = activeGameSave.value ?: return
        val board = SaveManager.deserializeBoard(save.boardStateString)
        val initial = SaveManager.deserializeBoard(save.initialBoardSnapshotString)
        val history = SaveManager.deserializeHistory(save.historyStackString)
        if (board == null || initial == null || history == null ||
            board.size != save.gridSize || initial.size != save.gridSize ||
            history.any { it.size != save.gridSize } || save.levelId < 1 || save.par < 1 ||
            save.movesCount < 0 || GameRules.checkWinCondition(board)) {
            notify("Your saved puzzle could not be restored. A fresh stage is ready.")
            loadLevel(save.levelId.coerceAtLeast(1))
            return
        }
        session += 1
        engine.reset()
        levelId.value = save.levelId
        gridSize.value = save.gridSize
        par.value = save.par
        movesCount.value = save.movesCount
        boardState.value = board
        engine.initialBoardSnapshot = initial
        engine.historyStack.addAll(history)
        canUndo.value = history.isNotEmpty()
        timer.restore(save.secondsElapsed, save.countdownSecondsLeft)
        val best = allProgress.value.find { it.levelId == save.levelId }
        previousBestMoves.value = best?.bestMoves ?: 0
        bestTimeSeconds.value = best?.bestTimeSeconds ?: 0
        ready = true
        updateClock()
    }

    private fun snapshot(): ActiveGameSave? {
        if (!ready || winState.value) return null
        return ActiveGameSave(
            levelId = levelId.value, gridSize = gridSize.value, par = par.value,
            movesCount = movesCount.value,
            boardStateString = SaveManager.serializeBoard(boardState.value ?: return null),
            initialBoardSnapshotString = SaveManager.serializeBoard(engine.initialBoardSnapshot ?: return null),
            secondsElapsed = secondsElapsed.value,
            historyStackString = SaveManager.serializeHistory(engine.historyStack),
            countdownSecondsLeft = countdownSecondsLeft.value,
        )
    }

    private fun scheduleSave(immediate: Boolean) {
        val save = snapshot() ?: return
        activeGameSave.value = save
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            if (!immediate) delay(500)
            persistenceMutex.withLock { repository.saveActiveGame(save) }
        }
    }

    fun saveNow() = scheduleSave(true)

    fun tapCell(r: Int, c: Int, context: Context) {
        if (!ready || winState.value || isTimeUp.value || overlayOpen || busy.value) return
        val previous = boardState.value ?: return
        if (r !in previous.indices || c !in previous.indices) return
        engine.historyStack.push(previous.deepCopy())
        // Bound the undo archive, not the player's move count.
        if (engine.historyStack.size > 500) engine.historyStack.removeAt(0)
        val board = previous.deepCopy()
        GameRules.applyTap(board, r, c)
        boardState.value = board
        movesCount.value += 1
        canUndo.value = true
        highlightedHintCell.value = null
        if (userStats.value?.soundEnabled == true) SoundEffectPlayer.playTap()
        if (userStats.value?.hapticEnabled == true) hapticController.vibrate(context, 25)
        if (GameRules.checkWinCondition(board)) completeLevel() else scheduleSave(false)
    }

    private fun completeLevel() {
        winState.value = true
        timer.pause()
        starredScore.value = GameRules.calculateStars(movesCount.value, par.value)
        if (userStats.value?.soundEnabled == true) SoundEffectPlayer.playWin()
        saveJob?.cancel()
        activeGameSave.value = null
        val id = levelId.value
        val stars = starredScore.value
        val moves = movesCount.value
        val elapsed = secondsElapsed.value
        viewModelScope.launch {
            persistenceMutex.withLock {
                repository.deleteActiveGameSave()
                repository.saveLevelProgress(id, stars, moves, elapsed)
            }
        }
    }

    fun undoMove() {
        if (busy.value || winState.value || isTimeUp.value || engine.historyStack.isEmpty()) return
        busy.value = true
        val token = session
        viewModelScope.launch {
            try {
                if (repository.useUndo() && token == session && engine.historyStack.isNotEmpty()) {
                    boardState.value = engine.historyStack.pop().deepCopy()
                    movesCount.value = (movesCount.value - 1).coerceAtLeast(0)
                    highlightedHintCell.value = null
                    canUndo.value = engine.historyStack.isNotEmpty()
                    saveNow()
                }
            } finally { busy.value = false }
        }
    }

    fun resetLevel() {
        val initial = engine.initialBoardSnapshot ?: return
        session += 1
        hintJob?.cancel()
        engine.reset()
        boardState.value = initial.deepCopy()
        canUndo.value = false
        timer.reset(par.value)
        saveNow()
        updateClock()
    }

    fun triggerHint() {
        if (busy.value || winState.value || isTimeUp.value) return
        val board = boardState.value?.deepCopy() ?: return
        val token = session
        busy.value = true
        viewModelScope.launch {
            try {
                val hint = withContext(Dispatchers.Default) { GameSolver.getBestHintCell(board, board.size) }
                if (token == session && hint != null && repository.useHint()) {
                    highlightedHintCell.value = hint
                    hintJob?.cancel()
                    hintJob = viewModelScope.launch { delay(4000); highlightedHintCell.value = null }
                }
            } finally { busy.value = false }
        }
    }

    fun earnReward(type: String, amount: Int) {
        if (type == "time") {
            timer.addSeconds(30)
            saveNow()
            updateClock()
        } else {
            viewModelScope.launch {
                when (type) {
                    "hint" -> repository.addHints(amount)
                    "undo" -> repository.addUndos(amount)
                    "skip" -> repository.addSkips(amount)
                }
            }
        }
    }

    fun skipLevel() {
        if (busy.value) return
        busy.value = true
        val token = session
        viewModelScope.launch {
            try {
                if (repository.useSkip() && token == session) {
                    persistenceMutex.withLock { repository.saveLevelProgress(levelId.value, 1, par.value, 0) }
                    loadNextLevel()
                }
            } finally { busy.value = false }
        }
    }

    fun switchTheme(name: String) { viewModelScope.launch { repository.updateTheme(VisualTheme.fromName(name).displayName) } }
    fun toggleSoundValue(value: Boolean) { viewModelScope.launch { repository.toggleSound(value) } }
    fun toggleMusicValue(value: Boolean) { viewModelScope.launch { repository.toggleMusic(value) } }
    fun toggleHapticValue(value: Boolean) { viewModelScope.launch { repository.toggleHaptic(value) } }
    fun completeTutorial() { viewModelScope.launch { repository.completeTutorial() } }

    fun resetAllGameData() {
        saveJob?.cancel()
        hintJob?.cancel()
        timer.pause()
        ready = false
        session += 1
        boardState.value = null
        activeGameSave.value = null
        viewModelScope.launch {
            persistenceMutex.withLock { repository.clearAllProgressAndResetStats() }
        }
    }

    override fun onCleared() {
        timer.pause()
        super.onCleared()
    }
}
