package com.example.di

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.repository.GameRepository
import com.example.platform.AdController
import com.example.platform.DefaultAdController
import com.example.platform.HapticController
import com.example.platform.DefaultHapticController

interface AppContainer {
    val gameRepository: GameRepository
    val adController: AdController
    val hapticController: HapticController
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val gameRepository: GameRepository by lazy {
        GameRepository(
            levelProgressDao = database.levelProgressDao(),
            userStatsDao = database.userStatsDao(),
            dailyChallengeDao = database.dailyChallengeDao(),
            activeGameSaveDao = database.activeGameSaveDao(),
            levelAttemptHistoryDao = database.levelAttemptHistoryDao()
        )
    }

    override val adController: AdController by lazy {
        DefaultAdController()
    }

    override val hapticController: HapticController by lazy {
        DefaultHapticController()
    }
}
