/**
 * DrawingRecord.kt
 *
 * Room database entity representing stored drawing metadata and corresponding image file references.
 *
 * Authors: Ryan Hoo (u1511875), Connor Kolodziej (u1458335), Luke Stansbury (u1422313)
 * Course: CS 4530
 */
package com.example.drawing_app

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// The image file is stored in the app's drawings directory.
@Entity(tableName = "drawings")
data class DrawingRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String
)
