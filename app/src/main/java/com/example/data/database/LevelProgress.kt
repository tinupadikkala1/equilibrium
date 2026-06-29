package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgress(
    @PrimaryKey val levelId: Int,
    val stars: Int,
    val bestMoves: Int,
    val completed: Boolean,
    val bestTimeSeconds: Int = 0
)
