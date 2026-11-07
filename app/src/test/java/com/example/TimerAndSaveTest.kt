package com.example

import com.example.ui.viewmodel.collaborators.SaveManager
import com.example.ui.viewmodel.collaborators.TimerController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Stack

@OptIn(ExperimentalCoroutinesApi::class)
class TimerAndSaveTest {
    @Test fun countdownPausesResumesExpiresAndAcceptsReward() = runTest {
        val timer = TimerController(backgroundScope)
        timer.reset(3)
        assertEquals(60, timer.countdownSecondsLeft.value)
        timer.resume()
        advanceTimeBy(5000); runCurrent()
        assertEquals(55, timer.countdownSecondsLeft.value)
        timer.pause()
        advanceTimeBy(20000); runCurrent()
        assertEquals(55, timer.countdownSecondsLeft.value)
        timer.restore(59, 1)
        timer.resume()
        advanceTimeBy(1000); runCurrent()
        assertTrue(timer.isTimeUp.value)
        timer.addSeconds(30)
        assertFalse(timer.isTimeUp.value)
        timer.resume()
        advanceTimeBy(1000); runCurrent()
        assertEquals(29, timer.countdownSecondsLeft.value)
        timer.pause()
    }

    @Test fun malformedOrRaggedSavesAreRejected() {
        listOf("", "1,2;3,X", "1,2,3;4,5,6;7,8", "1,2;3,4", "999999999999,2,3;4,5,6;7,8,9").forEach {
            assertNull(it, SaveManager.deserializeBoard(it))
        }
    }

    @Test fun boardAndHistoryRoundTrip() {
        val board = arrayOf(intArrayOf(-1, 5, 2), intArrayOf(3, 4, 5), intArrayOf(7, 8, 9))
        val encoded = SaveManager.serializeBoard(board)
        assertTrue(board.contentDeepEquals(SaveManager.deserializeBoard(encoded)))
        val history = Stack<Array<IntArray>>().apply { push(board); push(Array(3) { IntArray(3) { 5 } }) }
        val restored = SaveManager.deserializeHistory(SaveManager.serializeHistory(history))!!
        assertEquals(2, restored.size)
        assertTrue(history.pop().contentDeepEquals(restored.pop()))
    }
}
