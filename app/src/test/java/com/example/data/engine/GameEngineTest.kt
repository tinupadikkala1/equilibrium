package com.example.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun testTapTransformationAndWinCondition() {
        val board = Array(3) { IntArray(3) { 0 } }
        assertTrue(GameRules.checkWinCondition(board))

        // Tap center (1, 1)
        GameRules.applyTap(board, 1, 1)
        assertFalse(GameRules.checkWinCondition(board))
        assertEquals(-1, board[1][1])
        assertEquals(1, board[0][1])
        assertEquals(1, board[2][1])
        assertEquals(1, board[1][0])
        assertEquals(1, board[1][2])
        // Corners untouched
        assertEquals(0, board[0][0])
        assertEquals(0, board[0][2])
        assertEquals(0, board[2][0])
        assertEquals(0, board[2][2])

        // Reverse tap should bring it back to all 0s
        GameRules.applyReverseTap(board, 1, 1)
        assertTrue(GameRules.checkWinCondition(board))
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                assertEquals(0, board[r][c])
            }
        }
    }

    @Test
    fun testLevelGenerationDeterminism() {
        val level1A = LevelGenerator.generateLevel(1)
        val level1B = LevelGenerator.generateLevel(1)
        assertEquals(level1A.size, level1B.size)
        assertEquals(level1A.par, level1B.par)
        assertTrue(level1A.board.contentDeepEquals(level1B.board))

        val dailyA = LevelGenerator.generateDailyLevel(20261101)
        val dailyB = LevelGenerator.generateDailyLevel(20261101)
        assertEquals(dailyA.size, dailyB.size)
        assertEquals(dailyA.par, dailyB.par)
        assertTrue(dailyA.board.contentDeepEquals(dailyB.board))
    }

    @Test
    fun testGameSolverFindsWinningMove() {
        // Create a board that is 1 tap away from win
        val board = Array(3) { IntArray(3) { 0 } }
        GameRules.applyReverseTap(board, 1, 1)
        assertFalse(GameRules.checkWinCondition(board))

        val hint = GameSolver.getBestHintCell(board, 3)
        assertNotNull(hint)
        assertEquals(Pair(1, 1), hint)

        // Applying the hint solves the board
        GameRules.applyTap(board, hint!!.first, hint.second)
        assertTrue(GameRules.checkWinCondition(board))
    }

    @Test
    fun testLevelGeneratorGridSizes() {
        val level10 = LevelGenerator.generateLevel(10)
        assertEquals(3, level10.size)

        val level40 = LevelGenerator.generateLevel(40)
        assertEquals(4, level40.size)

        val level70 = LevelGenerator.generateLevel(70)
        assertEquals(5, level70.size)
    }
}
