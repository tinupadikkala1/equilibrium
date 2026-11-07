package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_game_save")
data class ActiveGameSave(
    @PrimaryKey val id: Int = 1,
    val levelId: Int,
    val gridSize: Int,
    val par: Int,
    val movesCount: Int,
    val boardStateString: String,
    val initialBoardSnapshotString: String,
    val secondsElapsed: Int,
    val historyStackString: String,
    val countdownSecondsLeft: Int
)
