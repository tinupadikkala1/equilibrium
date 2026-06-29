package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userStats: UserStats)

    @Query("UPDATE user_stats SET hints = hints - 1 WHERE id = 1 AND hints > 0")
    suspend fun decrementHint(): Int

    @Query("UPDATE user_stats SET undos = undos - 1 WHERE id = 1 AND undos > 0")
    suspend fun decrementUndo(): Int

    @Query("UPDATE user_stats SET skips = skips - 1 WHERE id = 1 AND skips > 0")
    suspend fun decrementSkip(): Int

    @Query("UPDATE user_stats SET hints = hints + :amount WHERE id = 1")
    suspend fun addHints(amount: Int)

    @Query("UPDATE user_stats SET undos = undos + :amount WHERE id = 1")
    suspend fun addUndos(amount: Int)

    @Query("UPDATE user_stats SET skips = skips + :amount WHERE id = 1")
    suspend fun addSkips(amount: Int)
}
