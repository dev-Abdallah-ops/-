package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Income::class,
        Expense::class,
        Bill::class,
        Goal::class,
        AppSettings::class,
        CustomCategory::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
}
