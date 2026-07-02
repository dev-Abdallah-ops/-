package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incomes",
    indices = [Index("timestamp"), Index("category")]
)
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val notes: String = ""
)
