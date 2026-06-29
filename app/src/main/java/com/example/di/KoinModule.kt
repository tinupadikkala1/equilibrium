package com.example.di

import com.example.data.database.AppDatabase
import com.example.data.repository.GameRepository
import com.example.platform.AdController
import com.example.platform.DefaultAdController
import com.example.platform.HapticController
import com.example.platform.DefaultHapticController
import com.example.ui.viewmodel.GameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single {
        val db = get<AppDatabase>()
        GameRepository(db.levelProgressDao(), db.userStatsDao(), db.dailyChallengeDao(), db.activeGameSaveDao(), db.levelAttemptHistoryDao())
    }
    single<AdController> { DefaultAdController() }
    single<HapticController> { DefaultHapticController() }
    viewModel { GameViewModel(get(), get(), get()) }
}
