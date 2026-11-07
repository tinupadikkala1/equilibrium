package com.example

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.*
import com.example.data.engine.LevelGenerator
import com.example.data.repository.GameRepository
import com.example.platform.AdController
import com.example.platform.HapticController
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GameSessionTest {
    private val ads = object : AdController {
        override fun initialize(activity: Activity) = Unit
        override fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) = onAdClosed()
        override fun showRewarded(activity: Activity, onRewardEarned: (Int) -> Unit, onAdClosed: () -> Unit) = onAdClosed()
    }
    private val haptics = object : HapticController {
        override fun vibrate(context: Context, durationMs: Long) = Unit
    }

    @Test fun sessionPreservesCountdownRewardsUndoAndCompletionAcrossResume() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.allowMainThreadQueries().build()
        val repo = GameRepository(db.levelProgressDao(), db.userStatsDao(), db.activeGameSaveDao(), db.levelAttemptHistoryDao())
        val store = ViewModelStore()
        try {
            db.userStatsDao().insertOrUpdate(UserStats(soundEnabled = false, musicEnabled = false, hapticEnabled = false, tutorialCompleted = true))
            val vm = GameViewModel(repo, ads, haptics)
            store.put("first", vm)
            runCurrent()
            vm.loadLevel(1)
            runCurrent()
            vm.setForeground(true); vm.setGameVisible(true)
            advanceTimeBy(5000); runCurrent()
            assertEquals(55, vm.countdownSecondsLeft.value)
            vm.setOverlayOpen(true)
            advanceTimeBy(8000); runCurrent()
            assertEquals(55, vm.countdownSecondsLeft.value)
            vm.setOverlayOpen(false)
            val initial = vm.boardState.value!!.map { it.clone() }.toTypedArray()
            vm.tapCell(0, 0, context)
            vm.undoMove()
            runCurrent()
            assertTrue(initial.contentDeepEquals(vm.boardState.value))
            assertEquals(4, db.userStatsDao().getUserStats()!!.undos)
            vm.earnReward("hint", 3)
            vm.earnReward("hint", 3)
            vm.toggleSoundValue(false)
            runCurrent()
            assertEquals(11, db.userStatsDao().getUserStats()!!.hints)
            vm.setForeground(false)
            vm.earnReward("time", 3)
            vm.switchTheme("Pop Machine")
            runCurrent()
            assertEquals(85, db.activeGameSaveDao().getActiveGameSave()!!.countdownSecondsLeft)
            assertEquals("Pop Machine", db.userStatsDao().getUserStats()!!.themeName)
            val restored = GameViewModel(repo, ads, haptics)
            store.put("restored", restored)
            runCurrent()
            restored.resumeActiveGame()
            runCurrent()
            assertEquals(85, restored.countdownSecondsLeft.value)
            assertTrue(initial.contentDeepEquals(restored.boardState.value))
            restored.resetLevel()
            runCurrent()
            assertEquals(60, restored.countdownSecondsLeft.value)
            val (_, taps) = LevelGenerator.generateWithWitness(1)
            taps.forEach { (r, c) -> restored.tapCell(r, c, context) }
            runCurrent()
            assertTrue(restored.winState.value)
            assertEquals(3, db.levelProgressDao().getLevelProgress(1)!!.stars)
            assertNull(db.activeGameSaveDao().getActiveGameSave())
            val moves = restored.movesCount.value
            restored.tapCell(0, 0, context)
            assertEquals(moves, restored.movesCount.value)
            restored.loadNextLevel()
            runCurrent()
            assertEquals(2, db.activeGameSaveDao().getActiveGameSave()!!.levelId)
            restored.setForeground(true)
            restored.setGameVisible(true)
            advanceTimeBy(1000000); runCurrent()
            assertTrue(restored.isTimeUp.value)
            val beforeTimeoutTap = restored.boardState.value!!.map { it.clone() }.toTypedArray()
            restored.tapCell(0, 0, context)
            assertTrue(beforeTimeoutTap.contentDeepEquals(restored.boardState.value))
            restored.setGameVisible(false)
            restored.resetAllGameData()
            runCurrent()
            assertNull(db.levelProgressDao().getLevelProgress(1))
            assertNull(db.activeGameSaveDao().getActiveGameSave())
            assertEquals(5, db.userStatsDao().getUserStats()!!.hints)
            val malformed = ActiveGameSave(
                levelId = 2, gridSize = 3, par = 4, movesCount = 1,
                boardStateString = "1,2,3;4,5;6,7,8",
                initialBoardSnapshotString = "5,5,5;5,6,5;5,5,5",
                secondsElapsed = 4, historyStackString = "", countdownSecondsLeft = 70,
            )
            db.activeGameSaveDao().insertOrUpdate(malformed)
            val recovered = GameViewModel(repo, ads, haptics)
            store.put("corrupt", recovered)
            runCurrent()
            recovered.resumeActiveGame()
            runCurrent()
            assertEquals(2, recovered.levelId.value)
            assertEquals(0, recovered.movesCount.value)
            assertNotNull(recovered.showToastMessage.value)
            assertTrue(LevelGenerator.generateLevel(2).board.contentDeepEquals(recovered.boardState.value))
        } finally {
            store.clear()
            db.close()
            Dispatchers.resetMain()
        }
    }
}
