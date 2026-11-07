package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LevelProgress::class, UserStats::class, ActiveGameSave::class, LevelAttemptHistory::class],
    version = 8, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun activeGameSaveDao(): ActiveGameSaveDao
    abstract fun levelAttemptHistoryDao(): LevelAttemptHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // Rebuild only the changed tables. Legacy daily records remain archived in SQLite;
        // campaign progress, attempts, currencies and preferences survive the upgrade.
        fun upgradeTo8(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE user_stats_v8 (id INTEGER NOT NULL PRIMARY KEY, hints INTEGER NOT NULL, undos INTEGER NOT NULL, skips INTEGER NOT NULL, currentStreak INTEGER NOT NULL, lastPlayDayOfYear INTEGER NOT NULL, soundEnabled INTEGER NOT NULL, hapticEnabled INTEGER NOT NULL, themeName TEXT NOT NULL, musicEnabled INTEGER NOT NULL, tutorialCompleted INTEGER NOT NULL)")
            db.execSQL("""INSERT INTO user_stats_v8 SELECT id, hints, undos, skips, currentStreak, lastPlayDayOfYear, soundEnabled, hapticEnabled,
                CASE WHEN themeName IN ('Pop Machine', 'Pastel Breeze', 'Minimal Mono') THEN 'Pop Machine' ELSE 'Orbital Glass' END,
                musicEnabled, 0 FROM user_stats""")
            db.execSQL("DROP TABLE user_stats")
            db.execSQL("ALTER TABLE user_stats_v8 RENAME TO user_stats")

            db.execSQL("CREATE TABLE active_game_save_v8 (id INTEGER NOT NULL PRIMARY KEY, levelId INTEGER NOT NULL, gridSize INTEGER NOT NULL, par INTEGER NOT NULL, movesCount INTEGER NOT NULL, boardStateString TEXT NOT NULL, initialBoardSnapshotString TEXT NOT NULL, secondsElapsed INTEGER NOT NULL, historyStackString TEXT NOT NULL, countdownSecondsLeft INTEGER NOT NULL)")
            db.execSQL("""INSERT INTO active_game_save_v8 SELECT id, levelId, gridSize, par, movesCount, boardStateString, initialBoardSnapshotString, secondsElapsed, historyStackString,
                CASE WHEN gameDifficulty = 'MASTER' THEN MAX(0, MAX(45, par * 20) - secondsElapsed) ELSE MAX(45, par * 20) END
                FROM active_game_save WHERE isDailyChallenge = 0""")
            db.execSQL("DROP TABLE active_game_save")
            db.execSQL("ALTER TABLE active_game_save_v8 RENAME TO active_game_save")
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) = upgradeTo8(db)
        }
        val MIGRATION_6_8 = object : Migration(6, 8) {
            override fun migrate(db: SupportSQLiteDatabase) = upgradeTo8(db)
        }

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "equilibrium_database")
                .addMigrations(MIGRATION_6_8, MIGRATION_7_8)
                .build().also { instance = it }
        }
    }
}
