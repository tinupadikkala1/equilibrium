package com.example.data.engine

import org.junit.Assert.*
import org.junit.Test

class LevelGeneratorTest {
    @Test fun currentStageBandsAndEndlessStagesRemainStable() {
        val bands = listOf(1 to 3, 20 to 3, 30 to 3, 31 to 4, 60 to 4, 61 to 5, 100 to 5, 101 to 3, 102 to 5, 104 to 4)
        bands.forEach { (id, size) -> assertEquals("stage $id", size, LevelGenerator.generateLevel(id).size) }
    }

    @Test fun firstThousandStagesAreDeterministicUnsolvedAndSolvableAtPar() {
        for (id in 1..1000) {
            val (level, witness) = LevelGenerator.generateWithWitness(id)
            assertEquals(level, LevelGenerator.generateLevel(id))
            assertFalse("Stage $id starts solved", GameRules.checkWinCondition(level.board))
            assertEquals(level.par, witness.size)
            val replay = level.board.map { it.clone() }.toTypedArray()
            witness.forEach { (r, c) -> GameRules.applyTap(replay, r, c) }
            assertTrue("Stage $id must solve at par", GameRules.checkWinCondition(replay))
        }
    }

    @Test fun generatedBoardsAreIndependent() {
        val a = LevelGenerator.generateLevel(1)
        val b = LevelGenerator.generateLevel(1)
        a.board[0][0] += 123
        assertNotEquals(a, b)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveStageIsRejected() { LevelGenerator.generateLevel(0) }
}
