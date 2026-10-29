package com.example.drawing_app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.BitmapFactory
import android.os.Build
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DrawingStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun bitmap() = Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.RED)
        setPixel(10, 15, Color.BLUE)
    }

    @Test
    fun savedDrawing_survivesClosingAndReopeningDatabase() = runBlocking {
        val name = "drawing-test-${UUID.randomUUID()}"
        var database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()
        try {
            database.drawingDAO().insert(Drawing(backing = bitmap()))
            database.close()
            database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()

            val drawings = database.drawingDAO().getAll().first()
            assertEquals(1, drawings.size)
            assertTrue(drawings.first().id > 0)
            val saved = drawings.first().backing
            assertEquals(20, saved.width)
            assertEquals(30, saved.height)
            assertEquals(Color.RED, saved.getPixel(0, 0))
            assertEquals(Color.BLUE, saved.getPixel(10, 15))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun repository_isTheSameInstanceForDifferentContexts() {
        assertSame(
            DrawingRepository.getRepository(context),
            DrawingRepository.getRepository(context.applicationContext)
        )
    }

    @Test
    fun deletedDrawing_staysDeletedAfterReopeningDatabase() = runBlocking {
        val name = "drawing-delete-test-${UUID.randomUUID()}"
        var database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()
        try {
            val deletedId = database.drawingDAO().insert(Drawing(backing = bitmap())).toInt()
            val keptId = database.drawingDAO().insert(Drawing(backing = bitmap())).toInt()
            database.drawingDAO().delete(Drawing(id = deletedId, backing = bitmap()))
            database.close()
            database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()

            assertEquals(listOf(keptId), database.drawingDAO().getAll().first().map { it.id })
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun galleryExport_writesReadablePng() = runBlocking {
        // Older Android versions request storage permission from the screen.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val uri = DrawingRepository.getRepository(context).saveToGallery(bitmap())
        try {
            val saved = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }!!
            assertEquals(20, saved.width)
            assertEquals(30, saved.height)
            assertEquals(Color.BLUE, saved.getPixel(10, 15))
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }
}
