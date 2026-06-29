package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelAttemptHistoryDao {
    @Insert
    suspend fun insertAttempt(attempt: LevelAttemptHistory)

    @Query("SELECT * FROM level_attempt_history WHERE levelId = :levelId AND isDaily = :isDaily ORDER BY timestamp ASC")
    fun getHistoryForLevel(levelId: Int, isDaily: Boolean): Flow<List<LevelAttemptHistory>>

    @Query("SELECT * FROM level_attempt_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<LevelAttemptHistory>>

    @Query("DELETE FROM level_attempt_history")
    suspend fun clearHistory()
}
