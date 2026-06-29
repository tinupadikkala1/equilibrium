package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LevelProgress::class, UserStats::class, DailyChallengeProgress::class, ActiveGameSave::class, LevelAttemptHistory::class],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun activeGameSaveDao(): ActiveGameSaveDao
    abstract fun levelAttemptHistoryDao(): LevelAttemptHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_stats ADD COLUMN zenHintsUsedToday INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN zenHintsLastResetDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "equilibrium_database"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
