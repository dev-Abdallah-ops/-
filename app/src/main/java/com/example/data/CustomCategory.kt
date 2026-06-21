package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val iconName: String
)
