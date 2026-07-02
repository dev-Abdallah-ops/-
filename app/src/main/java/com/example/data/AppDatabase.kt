package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Income::class,
        Expense::class,
        Bill::class,
        Goal::class,
        AppSettings::class,
        CustomCategory::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN isSmartAlertsEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
            }
        }

        val migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN hasCompletedOnboarding INTEGER NOT NULL DEFAULT 0")
            }
        }

        val migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN securityQuestion TEXT NOT NULL DEFAULT 'What is your favorite food?'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN securityAnswer TEXT NOT NULL DEFAULT 'Pizza'")
            }
        }

        val migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe no-op migration as requested
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance-database"
                )
                    .addMigrations(migration4To5, migration5To6, migration6To7, migration7To8, migration8To9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
