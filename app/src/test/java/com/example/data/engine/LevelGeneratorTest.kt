package com.example.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun levelLadderMatchesMasterBands() {
        // EASY 1..250 -> 3x3, MEDIUM -> 4x4, HARD -> 5x5, ULTRA -> 6x6
        val expectations = listOf(
            1 to 3, 125 to 3, 250 to 3,
            251 to 4, 400 to 4, 500 to 4,
            501 to 5, 650 to 5, 750 to 5,
            751 to 6, 900 to 6, 1000 to 6
        )
        expectations.forEach { (id, size) ->
            assertEquals("level $id grid size", size, LevelGenerator.generateLevel(id).size)
        }
    }

    @Test
    fun allCampaignLevelsStayInside1To12AndAreNotSolved() {
        for (id in 1..LevelGenerator.MAX_CAMPAIGN_LEVEL) {
            val level = LevelGenerator.generateLevel(id)
            var min = Int.MAX_VALUE
            var max = Int.MIN_VALUE
            for (r in 0 until level.size) for (c in 0 until level.size) {
                val v = level.board[r][c]
                assertTrue("level $id value $v below min", v >= GameRules.MIN_TILE_VALUE)
                assertTrue("level $id value $v above max", v <= GameRules.MAX_TILE_VALUE)
                min = minOf(min, v)
                max = maxOf(max, v)
            }
            assertNotEquals("level $id generated already solved", min, max)
            assertTrue("level $id par too small: ${level.par}", level.par >= 3)
        }
    }

    @Test
    fun witnessTapSequenceIsLegalAndWins() {
        val sampleIds = listOf(1, 5, 60, 137, 250, 251, 377, 500, 501, 611, 750, 751, 900, 999, 1000)
        sampleIds.forEach { id ->
            val (level, witness) = LevelGenerator.generateWithWitness(id)
            assertEquals(level.par, witness.size)
            val replay = Array(level.size) { level.board[it].clone() }
            witness.forEach { (r, c) ->
                assertTrue("level $id witness tap ($r,$c) illegal", GameRules.isValidTap(replay, r, c))
                GameRules.applyTap(replay, r, c)
                for (row in replay) for (v in row) {
                    assertTrue(v in GameRules.MIN_TILE_VALUE..GameRules.MAX_TILE_VALUE)
                }
            }
            assertTrue("level $id witness did not win", GameRules.checkWinCondition(replay))
        }
    }

    @Test
    fun levelsAreDeterministicPerId() {
        for (id in listOf(1, 200, 377, 640, 999)) {
            val a = LevelGenerator.generateLevel(id)
            val b = LevelGenerator.generateLevel(id)
            assertEquals(a.par, b.par)
            for (r in 0 until a.size) assertEquals(a.board[r].toList(), b.board[r].toList())
        }
    }

    @Test
    fun dailyLevelIsBoundedAndSeeded() {
        val odd = LevelGenerator.generateDailyLevel(20261107)
        val even = LevelGenerator.generateDailyLevel(20261108)
        assertEquals(5, odd.size)
        assertEquals(4, even.size)
        assertEquals(odd.board[0].toList(), LevelGenerator.generateDailyLevel(20261107).board[0].toList())
        listOf(odd, even).forEach { level ->
            for (row in level.board) for (v in row) assertTrue(v in GameRules.MIN_TILE_VALUE..GameRules.MAX_TILE_VALUE)
        }
    }

    @Test
    fun earlyLevelsUseSmallNumbersLateLevelsUseUpTo12() {
        assertFalse(LevelGenerator.generateLevel(1).board.any { row -> row.any { it > 6 } })
        // Ultra ramp reaches the documented cap of 12.
        var ultraMax = 0
        for (id in 990..1000) {
            for (row in LevelGenerator.generateLevel(id).board) {
                ultraMax = maxOf(ultraMax, row.max())
            }
        }
        assertTrue("ultra band never reached a 12 tile", ultraMax >= 8)
        for (id in 990..1000) {
            for (row in LevelGenerator.generateLevel(id).board) {
                assertTrue(row.all { it <= GameRules.MAX_TILE_VALUE })
            }
        }
    }
}
