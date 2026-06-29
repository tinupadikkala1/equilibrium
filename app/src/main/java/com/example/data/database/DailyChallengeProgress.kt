package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenge_progress")
data class DailyChallengeProgress(
    @PrimaryKey val dateKey: Int, // Format: YYYYMMDD (e.g. 20260617)
    val completed: Boolean,
    val movesTaken: Int,
    val timeTakenSeconds: Int,
    val stars: Int
)
