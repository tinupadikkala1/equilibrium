package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DatabaseMigrationTest {
    @Test fun legacyInstallKeepsProgressTokensPreferencesAndCampaignSave() = migrateLegacy(7)
    @Test fun versionSixUpgradesWithoutDestructiveFallback() = migrateLegacy(6)

    private fun migrateLegacy(version: Int) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${System.nanoTime()}"
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val legacyColumns = if (version == 7) ", zenHintsUsedToday INTEGER NOT NULL, zenHintsLastResetDay INTEGER NOT NULL" else ""
                    val legacyValues = if (version == 7) ",3,20000" else ""
                    db.execSQL("CREATE TABLE user_stats (id INTEGER NOT NULL PRIMARY KEY, hints INTEGER NOT NULL, undos INTEGER NOT NULL, skips INTEGER NOT NULL, currentStreak INTEGER NOT NULL, lastPlayDayOfYear INTEGER NOT NULL, soundEnabled INTEGER NOT NULL, hapticEnabled INTEGER NOT NULL, themeName TEXT NOT NULL, musicEnabled INTEGER NOT NULL$legacyColumns)")
                    db.execSQL("INSERT INTO user_stats VALUES (1,17,9,2,7,20000,1,0,'Pastel Breeze',0$legacyValues)")
                    db.execSQL("CREATE TABLE active_game_save (id INTEGER NOT NULL PRIMARY KEY, levelId INTEGER NOT NULL, gridSize INTEGER NOT NULL, par INTEGER NOT NULL, movesCount INTEGER NOT NULL, boardStateString TEXT NOT NULL, initialBoardSnapshotString TEXT NOT NULL, secondsElapsed INTEGER NOT NULL, isDailyChallenge INTEGER NOT NULL, dailyDateKey INTEGER NOT NULL, gameDifficulty TEXT NOT NULL, historyStackString TEXT NOT NULL)")
                    db.execSQL("INSERT INTO active_game_save VALUES (1,2,3,4,1,'5,4,5;4,6,4;5,4,5','5,4,5;4,6,4;5,4,5',10,0,0,'MASTER','')")
                    db.execSQL("CREATE TABLE level_progress (levelId INTEGER NOT NULL PRIMARY KEY, stars INTEGER NOT NULL, bestMoves INTEGER NOT NULL, completed INTEGER NOT NULL, bestTimeSeconds INTEGER NOT NULL)")
                    db.execSQL("INSERT INTO level_progress VALUES (1,3,4,1,21)")
                    db.execSQL("CREATE TABLE level_attempt_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, levelId INTEGER NOT NULL, isDaily INTEGER NOT NULL, movesTaken INTEGER NOT NULL, timeTakenSeconds INTEGER NOT NULL, stars INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
                    db.execSQL("INSERT INTO level_attempt_history VALUES (1,1,0,4,21,3,1234)")
                    db.execSQL("CREATE TABLE daily_challenge_progress (dateKey INTEGER NOT NULL PRIMARY KEY)")
                    db.execSQL("INSERT INTO daily_challenge_progress VALUES (20261107)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.writableDatabase
        helper.close()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_6_8, AppDatabase.MIGRATION_7_8).allowMainThreadQueries().build()
        try {
            val stats = migrated.userStatsDao().getUserStats()!!
            assertEquals(17, stats.hints)
            assertEquals(9, stats.undos)
            assertEquals("Pop Machine", stats.themeName)
            assertFalse(stats.musicEnabled)
            assertFalse(stats.tutorialCompleted)
            assertEquals(3, migrated.levelProgressDao().getLevelProgress(1)!!.stars)
            val save = migrated.activeGameSaveDao().getActiveGameSave()!!
            assertEquals(2, save.levelId)
            assertEquals(70, save.countdownSecondsLeft)
            migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM daily_challenge_progress").use {
                it.moveToFirst(); assertEquals(1, it.getInt(0))
            }
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
