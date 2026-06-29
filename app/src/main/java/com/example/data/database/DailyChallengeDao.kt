package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenge_progress ORDER BY dateKey DESC")
    fun getAllDailyProgress(): Flow<List<DailyChallengeProgress>>

    @Query("SELECT * FROM daily_challenge_progress WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyProgress(dateKey: Int): DailyChallengeProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: DailyChallengeProgress)

    @Query("DELETE FROM daily_challenge_progress")
    suspend fun deleteAll()
}
