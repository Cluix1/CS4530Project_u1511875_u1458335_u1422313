package com.example.drawing_app

import android.content.Context
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import java.io.File
import java.io.IOException
import java.util.UUID

// Keep drawings saved before images were moved out of Room.
class DrawingMigration(context: Context) : Migration(1, 2) {
    private val directory = File(context.applicationContext.filesDir, "drawings")

    override suspend fun migrate(connection: SQLiteConnection) {
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Could not create drawings folder")
        val createdFiles = mutableListOf<File>()
        try {
            connection.prepare("CREATE TABLE drawings_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fileName TEXT NOT NULL)").use {
                it.step()
            }
            connection.prepare("SELECT id, backing FROM drawings").use { rows ->
                while (rows.step()) {
                    val file = File(directory, "Drawing_${UUID.randomUUID()}.png")
                    createdFiles.add(file)
                    file.writeBytes(rows.getBlob(1))
                    connection.prepare("INSERT INTO drawings_new (id, fileName) VALUES (?, ?)").use { insert ->
                        insert.bindLong(1, rows.getLong(0))
                        insert.bindText(2, file.name)
                        insert.step()
                    }
                }
            }
            connection.prepare("DROP TABLE drawings").use { it.step() }
            connection.prepare("ALTER TABLE drawings_new RENAME TO drawings").use { it.step() }
        } catch (e: Exception) {
            createdFiles.forEach { it.delete() }
            throw e
        }
    }
}
