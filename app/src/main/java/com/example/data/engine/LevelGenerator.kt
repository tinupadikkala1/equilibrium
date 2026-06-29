package com.example.data.engine

import java.util.Random

data class Level(
    val levelId: Int,
    val size: Int,
    val par: Int,
    val board: Array<IntArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Level
        return levelId == other.levelId && size == other.size && par == other.par && board.contentDeepEquals(other.board)
    }

    override fun hashCode(): Int {
        var result = levelId
        result = 31 * result + size
        result = 31 * result + par
        result = 31 * result + board.contentDeepHashCode()
        return result
    }
}

object LevelGenerator {

    fun generateDailyLevel(dateKey: Int): Level {
        val size = if (dateKey % 2 == 0) 4 else 5
        val minScramble = if (size == 4) 6 else 8
        val maxScramble = if (size == 4) 9 else 11
        return generateWithParameters(dateKey, size, minScramble, maxScramble)
    }

    fun generateLevel(levelId: Int): Level {
        // Set parameters based on level range
        val size: Int
        val minScramble: Int
        val maxScramble: Int

        when {
            levelId <= 20 -> {
                size = 3
                minScramble = 3
                maxScramble = 5
            }
            levelId <= 30 -> {
                size = 3
                minScramble = 6
                maxScramble = 9
            }
            levelId <= 50 -> {
                size = 4
                minScramble = 4
                maxScramble = 7
            }
            levelId <= 60 -> {
                size = 4
                minScramble = 8
                maxScramble = 12
            }
            levelId <= 80 -> {
                size = 5
                minScramble = 5
                maxScramble = 8
            }
            levelId <= 100 -> {
                size = 5
                minScramble = 9
                maxScramble = 15
            }
            else -> {
                // Endless procedurally difficulty scaling!
                size = if (levelId % 3 == 0) 5 else if (levelId % 2 == 0) 4 else 3
                minScramble = minOf(6 + (levelId - 100) / 10, 15)
                maxScramble = minOf(12 + (levelId - 100) / 5, 25)
            }
        }

        return generateWithParameters(levelId, size, minScramble, maxScramble)
    }

    private fun generateWithParameters(levelId: Int, size: Int, minScramble: Int, maxScramble: Int, attempt: Int = 0): Level {
        val random = Random(levelId.toLong() * 98765L + attempt * 12345L + size * 777L)
        
        // Step 1: Start with target level 5
        val board = Array(size) { IntArray(size) { 5 } }
        
        val totalMoves = random.nextInt(maxScramble - minScramble + 1) + minScramble
        
        // Step 2: Scramble from the solved state (apply reverse tap)
        for (i in 0 until totalMoves) {
            val r = random.nextInt(size)
            val c = random.nextInt(size)
            GameRules.applyReverseTap(board, r, c)
        }
        
        // Step 3: Check if accidentally solved
        var minVal = Int.MAX_VALUE
        var maxVal = Int.MIN_VALUE
        for (r in 0 until size) {
            for (c in 0 until size) {
                val value = board[r][c]
                if (value < minVal) minVal = value
                if (value > maxVal) maxVal = value
            }
        }
        
        if (minVal == maxVal) {
            if (attempt >= 10) {
                // Fallback: apply one extra reverse tap to guarantee non-uniform board
                GameRules.applyReverseTap(board, 0, 0)
                return Level(levelId = levelId, size = size, par = 1, board = board)
            }
            return generateWithParameters(levelId, size, minScramble, maxScramble, attempt + 1)
        }
        
        return Level(
            levelId = levelId,
            size = size,
            par = totalMoves,
            board = board
        )
    }

}
