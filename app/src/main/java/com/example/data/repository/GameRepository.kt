package com.example.data.repository

import com.example.data.database.LevelProgress
import com.example.data.database.LevelProgressDao
import com.example.data.database.UserStats
import com.example.data.database.UserStatsDao
import com.example.data.database.DailyChallengeDao
import com.example.data.database.DailyChallengeProgress
import com.example.data.database.ActiveGameSave
import com.example.data.database.ActiveGameSaveDao
import com.example.data.database.LevelAttemptHistory
import com.example.data.database.LevelAttemptHistoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class GameRepository(
    private val levelProgressDao: LevelProgressDao,
    private val userStatsDao: UserStatsDao,
    private val dailyChallengeDao: DailyChallengeDao,
    private val activeGameSaveDao: ActiveGameSaveDao,
    private val levelAttemptHistoryDao: LevelAttemptHistoryDao
) {
    val allLevelProgress: Flow<List<LevelProgress>> = levelProgressDao.getAllLevelProgress()
    val allDailyProgress: Flow<List<DailyChallengeProgress>> = dailyChallengeDao.getAllDailyProgress()
    val userStatsFlow: Flow<UserStats?> = userStatsDao.getUserStatsFlow()

    suspend fun getActiveGameSave(): ActiveGameSave? = activeGameSaveDao.getActiveGameSave()

    suspend fun saveActiveGame(activeGameSave: ActiveGameSave) {
        activeGameSaveDao.insertOrUpdate(activeGameSave)
    }

    suspend fun deleteActiveGameSave() {
        activeGameSaveDao.deleteActiveGameSave()
    }

    suspend fun getOrCreateUserStats(): UserStats {
        val existing = userStatsDao.getUserStats()
        if (existing != null) {
            return existing
        }
        val defaultStats = UserStats()
        userStatsDao.insertOrUpdate(defaultStats)
        return defaultStats
    }

    suspend fun saveLevelProgress(levelId: Int, stars: Int, movesTaken: Int, timeTakenSeconds: Int) {
        val existing = levelProgressDao.getLevelProgress(levelId)
        val bestMoves = if (existing == null) {
            movesTaken
        } else {
            if (existing.bestMoves == 0) movesTaken else minOf(existing.bestMoves, movesTaken)
        }
        val maxStars = if (existing == null) {
            stars
        } else {
            maxOf(existing.stars, stars)
        }
        val bestTime = if (existing == null) {
            timeTakenSeconds
        } else {
            if (existing.bestTimeSeconds == 0) {
                timeTakenSeconds
            } else if (timeTakenSeconds == 0) {
                existing.bestTimeSeconds
            } else {
                minOf(existing.bestTimeSeconds, timeTakenSeconds)
            }
        }
        
        levelProgressDao.insertOrUpdate(
            LevelProgress(
                levelId = levelId,
                stars = maxStars,
                bestMoves = bestMoves,
                completed = true,
                bestTimeSeconds = bestTime
            )
        )
        levelAttemptHistoryDao.insertAttempt(
            LevelAttemptHistory(
                levelId = levelId,
                isDaily = false,
                movesTaken = movesTaken,
                timeTakenSeconds = timeTakenSeconds,
                stars = stars
            )
        )
    }

    suspend fun useUndo(): Boolean {
        return userStatsDao.decrementUndo() > 0
    }

    suspend fun addUndos(amount: Int) {
        userStatsDao.addUndos(amount)
    }

    suspend fun useHint(): Boolean {
        return userStatsDao.decrementHint() > 0
    }

    suspend fun useZenHint(): Boolean {
        val stats = getOrCreateUserStats()
        val todayEpochDay = (System.currentTimeMillis() / (24 * 60 * 60 * 1000)).toInt()
        val usedToday = if (stats.zenHintsLastResetDay == todayEpochDay) stats.zenHintsUsedToday else 0
        if (usedToday < 10) {
            userStatsDao.insertOrUpdate(stats.copy(zenHintsUsedToday = usedToday + 1, zenHintsLastResetDay = todayEpochDay))
            return true
        }
        return false
    }

    suspend fun getZenHintsRemaining(): Int {
        val stats = getOrCreateUserStats()
        val todayEpochDay = (System.currentTimeMillis() / (24 * 60 * 60 * 1000)).toInt()
        val usedToday = if (stats.zenHintsLastResetDay == todayEpochDay) stats.zenHintsUsedToday else 0
        return (10 - usedToday).coerceAtLeast(0)
    }

    suspend fun addHints(amount: Int) {
        userStatsDao.addHints(amount)
    }

    suspend fun useSkip(): Boolean {
        return userStatsDao.decrementSkip() > 0
    }

    suspend fun addSkips(amount: Int) {
        userStatsDao.addSkips(amount)
    }

    suspend fun updateTheme(themeName: String) {
        val stats = getOrCreateUserStats()
        userStatsDao.insertOrUpdate(stats.copy(themeName = themeName))
    }

    suspend fun toggleSound(enabled: Boolean) {
        val stats = getOrCreateUserStats()
        userStatsDao.insertOrUpdate(stats.copy(soundEnabled = enabled))
    }

    suspend fun toggleMusic(enabled: Boolean) {
        val stats = getOrCreateUserStats()
        userStatsDao.insertOrUpdate(stats.copy(musicEnabled = enabled))
    }

    suspend fun toggleHaptic(enabled: Boolean) {
        val stats = getOrCreateUserStats()
        userStatsDao.insertOrUpdate(stats.copy(hapticEnabled = enabled))
    }

    suspend fun checkDailyStreak() {
        val stats = getOrCreateUserStats()
        val todayEpochDay = (System.currentTimeMillis() / (24 * 60 * 60 * 1000)).toInt()
        val lastPlay = stats.lastPlayDayOfYear

        if (lastPlay == todayEpochDay) {
            return // already played today, streak remains same
        }

        val newStreak = if (lastPlay == todayEpochDay - 1) {
            stats.currentStreak + 1
        } else {
            1 // streak broken
        }

        // Reward: if streak incremented, add bonus hints/undos
        val bonusHints = if (newStreak > stats.currentStreak) 1 else 0
        val bonusUndos = if (newStreak > stats.currentStreak) 1 else 0

        userStatsDao.insertOrUpdate(
            stats.copy(
                currentStreak = newStreak,
                lastPlayDayOfYear = todayEpochDay,
                hints = stats.hints + bonusHints,
                undos = stats.undos + bonusUndos
            )
        )
    }

    suspend fun saveDailyProgress(dateKey: Int, completed: Boolean, movesTaken: Int, timeTakenSeconds: Int, stars: Int) {
        val existing = dailyChallengeDao.getDailyProgress(dateKey)
        val bestMoves = if (existing == null) {
            movesTaken
        } else {
            if (existing.movesTaken == 0) movesTaken else minOf(existing.movesTaken, movesTaken)
        }
        val maxStars = if (existing == null) {
            stars
        } else {
            maxOf(existing.stars, stars)
        }
        val bestTime = if (existing == null) {
            timeTakenSeconds
        } else {
            if (existing.timeTakenSeconds == 0) {
                timeTakenSeconds
            } else if (timeTakenSeconds == 0) {
                existing.timeTakenSeconds
            } else {
                minOf(existing.timeTakenSeconds, timeTakenSeconds)
            }
        }

        dailyChallengeDao.insertOrUpdate(
            DailyChallengeProgress(
                dateKey = dateKey,
                completed = completed,
                movesTaken = bestMoves,
                timeTakenSeconds = bestTime,
                stars = maxStars
            )
        )
        levelAttemptHistoryDao.insertAttempt(
            LevelAttemptHistory(
                levelId = dateKey,
                isDaily = true,
                movesTaken = movesTaken,
                timeTakenSeconds = timeTakenSeconds,
                stars = stars
            )
        )
    }

    suspend fun clearAllProgressAndResetStats() {
        levelProgressDao.deleteAll()
        dailyChallengeDao.deleteAll()
        activeGameSaveDao.deleteActiveGameSave()
        val defaultStats = UserStats(
            id = 1,
            themeName = "Neon Pulse",
            hints = 5,
            undos = 5,
            skips = 1,
            soundEnabled = true,
            hapticEnabled = true,
            musicEnabled = true,
            currentStreak = 1,
            lastPlayDayOfYear = 0
        )
        userStatsDao.insertOrUpdate(defaultStats)
        levelAttemptHistoryDao.clearHistory()
    }

    fun getHistoryForLevel(levelId: Int, isDaily: Boolean): Flow<List<LevelAttemptHistory>> {
        return levelAttemptHistoryDao.getHistoryForLevel(levelId, isDaily)
    }

    fun getAllHistory(): Flow<List<LevelAttemptHistory>> {
        return levelAttemptHistoryDao.getAllHistory()
    }
}
