package com.example.drawing_app

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// The image file is stored in the app's drawings directory.
@Entity(tableName = "drawings")
data class DrawingRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String
)
