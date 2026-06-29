package com.example.data.engine

object GameSolver {

    fun getBestHintCell(currentBoard: Array<IntArray>, size: Int): Pair<Int, Int>? {
        if (size == 0) return null

        // First check: can any single tap solve it?
        for (r in 0 until size) {
            for (c in 0 until size) {
                val temp = Array(size) { row -> currentBoard[row].clone() }
                GameRules.applyTap(temp, r, c)
                if (GameRules.checkWinCondition(temp)) return Pair(r, c)
            }
        }

        // For small grids near solution (spread <= 3), try 2-move lookahead
        val spread = getSpread(currentBoard, size)
        if (size <= 4 && spread <= 3) {
            val twoMoveResult = twoMoveLookahead(currentBoard, size)
            if (twoMoveResult != null) return twoMoveResult
        }

        // Primary strategy: pick the cell whose tap reduces variance the most
        // With tie-breaking: prefer cells adjacent to the max-value cell
        val maxCell = findMaxCell(currentBoard, size)
        var bestCell: Pair<Int, Int>? = null
        var bestScore = Float.MAX_VALUE

        for (r in 0 until size) {
            for (c in 0 until size) {
                val temp = Array(size) { row -> currentBoard[row].clone() }
                GameRules.applyTap(temp, r, c)

                val variance = computeVariance(temp, size)
                // Small bonus for tapping the highest-value cell (it decreases by 1)
                val bonus = if (r == maxCell.first && c == maxCell.second) -0.5f else 0f
                val score = variance + bonus

                if (score < bestScore) {
                    bestScore = score
                    bestCell = Pair(r, c)
                }
            }
        }
        return bestCell
    }

    private fun twoMoveLookahead(board: Array<IntArray>, size: Int): Pair<Int, Int>? {
        var bestCell: Pair<Int, Int>? = null
        var bestVariance = Float.MAX_VALUE

        for (r1 in 0 until size) {
            for (c1 in 0 until size) {
                val after1 = Array(size) { row -> board[row].clone() }
                GameRules.applyTap(after1, r1, c1)
                if (GameRules.checkWinCondition(after1)) return Pair(r1, c1)

                // Find best second move
                for (r2 in 0 until size) {
                    for (c2 in 0 until size) {
                        val after2 = Array(size) { row -> after1[row].clone() }
                        GameRules.applyTap(after2, r2, c2)
                        if (GameRules.checkWinCondition(after2)) {
                            return Pair(r1, c1) // First move of a 2-move solution
                        }
                        val v = computeVariance(after2, size)
                        if (v < bestVariance) {
                            bestVariance = v
                            bestCell = Pair(r1, c1)
                        }
                    }
                }
            }
        }
        return bestCell
    }

    private fun computeVariance(board: Array<IntArray>, size: Int): Float {
        var sum = 0f
        for (i in 0 until size) for (j in 0 until size) sum += board[i][j].toFloat()
        val avg = sum / (size * size)
        var variance = 0f
        for (i in 0 until size) for (j in 0 until size) {
            val d = board[i][j].toFloat() - avg
            variance += d * d
        }
        return variance
    }

    private fun getSpread(board: Array<IntArray>, size: Int): Int {
        var min = Int.MAX_VALUE
        var max = Int.MIN_VALUE
        for (i in 0 until size) for (j in 0 until size) {
            val v = board[i][j]
            if (v < min) min = v
            if (v > max) max = v
        }
        return max - min
    }

    private fun findMaxCell(board: Array<IntArray>, size: Int): Pair<Int, Int> {
        var maxVal = Int.MIN_VALUE
        var maxR = 0
        var maxC = 0
        for (i in 0 until size) for (j in 0 until size) {
            if (board[i][j] > maxVal) {
                maxVal = board[i][j]
                maxR = i
                maxC = j
            }
        }
        return Pair(maxR, maxC)
    }
}
