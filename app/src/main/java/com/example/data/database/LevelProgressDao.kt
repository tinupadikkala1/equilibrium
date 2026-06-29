package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllLevelProgress(): Flow<List<LevelProgress>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId LIMIT 1")
    suspend fun getLevelProgress(levelId: Int): LevelProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(levelProgress: LevelProgress)

    @Query("DELETE FROM level_progress")
    suspend fun deleteAll()
}
