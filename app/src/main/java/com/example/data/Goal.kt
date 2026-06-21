package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val deadline: String = "", // e.g. "2026-12-31" or empty
    val colorHex: String = "#2ECC71",
    val iconName: String = "ic_car",
    val notes: String = ""
)
