package com.example.data.database

import androidx.room.*

@Dao
interface ActiveGameSaveDao {
    @Query("SELECT * FROM active_game_save WHERE id = 1 LIMIT 1")
    suspend fun getActiveGameSave(): ActiveGameSave?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(activeGameSave: ActiveGameSave)

    @Query("DELETE FROM active_game_save WHERE id = 1")
    suspend fun deleteActiveGameSave()
}
