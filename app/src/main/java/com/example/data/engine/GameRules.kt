package com.example.data.engine

object GameRules {
    /**
     * Applies the tap transformation formula:
     * - The tapped cell value decreases by 1.
     * - The values of its adjacent orthogonal neighbors increase by 1.
     */
    fun applyTap(board: Array<IntArray>, r: Int, c: Int) {
        val size = board.size
        board[r][c] -= 1

        val dr = intArrayOf(-1, 1, 0, 0)
        val dc = intArrayOf(0, 0, -1, 1)
        for (i in 0 until 4) {
            val nr = r + dr[i]
            val nc = c + dc[i]
            if (nr in 0 until size && nc in 0 until size) {
                board[nr][nc] += 1
            }
        }
    }

    /**
     * Applies the reverse tap transformation (used for level generation):
     * - The tapped cell value increases by 1.
     * - The values of its adjacent orthogonal neighbors decrease by 1.
     */
    fun applyReverseTap(board: Array<IntArray>, r: Int, c: Int) {
        val size = board.size
        board[r][c] += 1

        val dr = intArrayOf(-1, 1, 0, 0)
        val dc = intArrayOf(0, 0, -1, 1)
        for (i in 0 until 4) {
            val nr = r + dr[i]
            val nc = c + dc[i]
            if (nr in 0 until size && nc in 0 until size) {
                board[nr][nc] -= 1
            }
        }
    }

    /**
     * Checks if all values on the grid are balanced (i.e. equal).
     */
    fun checkWinCondition(board: Array<IntArray>): Boolean {
        val size = board.size
        if (size == 0) return true
        val firstVal = board[0][0]
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] != firstVal) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Computes the balance progress percentage indicator:
     * - Returns 1.0f if all elements are balanced.
     * - Otherwise returns a progress value in range [0.0f, 1.0f] based on difference between min and max values.
     */
    fun calculateProgress(board: Array<IntArray>): Float {
        val size = board.size
        if (size == 0) return 1.0f
        var minVal = Int.MAX_VALUE
        var maxVal = Int.MIN_VALUE
        for (r in 0 until size) {
            for (c in 0 until size) {
                val v = board[r][c]
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }
        val maxSpread = (size * 2).toFloat()
        return if (minVal == maxVal) 1.0f
        else (1.0f - ((maxVal - minVal).toFloat() / maxSpread)).coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculates the star rating based on moves taken relative to level par.
     */
    fun calculateStars(moves: Int, parScore: Int): Int {
        return when {
            moves <= parScore -> 3
            moves <= parScore + 2 -> 2
            else -> 1
        }
    }
}
