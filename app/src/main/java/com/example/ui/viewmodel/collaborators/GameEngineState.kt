package com.example.ui.viewmodel.collaborators

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Stack

class GameEngineState {
    val levelId = MutableStateFlow(1)
    val gridSize = MutableStateFlow(3)
    val par = MutableStateFlow(3)
    val movesCount = MutableStateFlow(0)
    val boardState = MutableStateFlow<Array<IntArray>?>(null)
    val winState = MutableStateFlow(false)
    val starredScore = MutableStateFlow(0)
    val highlightedHintCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val hintsUsedThisLevel = MutableStateFlow(0)

    var initialBoardSnapshot: Array<IntArray>? = null
    val historyStack = Stack<Array<IntArray>>()

    fun reset() {
        movesCount.value = 0
        winState.value = false
        starredScore.value = 0
        highlightedHintCell.value = null
        historyStack.clear()
        hintsUsedThisLevel.value = 0
    }
}
