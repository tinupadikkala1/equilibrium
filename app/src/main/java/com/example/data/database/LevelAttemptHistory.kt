package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_attempt_history")
data class LevelAttemptHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val levelId: Int,
    val isDaily: Boolean,
    val movesTaken: Int,
    val timeTakenSeconds: Int,
    val stars: Int,
    val timestamp: Long = System.currentTimeMillis()
)
