package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val hints: Int = 5,
    val undos: Int = 5,
    val skips: Int = 1,
    val currentStreak: Int = 1,
    val lastPlayDayOfYear: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val themeName: String = "Neon Pulse",
    val musicEnabled: Boolean = true,
    val zenHintsUsedToday: Int = 0,
    val zenHintsLastResetDay: Int = 0
)
