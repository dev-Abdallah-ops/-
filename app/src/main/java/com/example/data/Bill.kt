package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val frequency: String, // "Monthly", "Weekly", "Yearly"
    val dueDay: Int, // Day of month or specific counter
    val category: String,
    val notes: String = "",
    val isPaid: Boolean = false
)
