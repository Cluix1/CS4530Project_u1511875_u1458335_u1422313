package com.example.drawing_app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.BitmapFactory
import android.os.Build
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.io.File
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class DrawingStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun bitmap() = Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.RED)
        setPixel(10, 15, Color.BLUE)
    }

    private fun testImage(): File {
        val directory = File(context.filesDir, "drawings")
        directory.mkdirs()
        val file = File.createTempFile("test-", ".png", directory)
        file.outputStream().use { bitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    @Test
    fun savedDrawing_survivesClosingAndReopeningDatabase() = runBlocking {
        val name = "drawing-test-${System.currentTimeMillis()}"
        val imageFile = testImage()
        var database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()
        try {
            database.drawingDAO().insert(DrawingRecord(fileName = imageFile.name))
            database.close()
            database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()

            val drawings = database.drawingDAO().getAll().first()
            assertEquals(1, drawings.size)
            assertTrue(drawings.first().id > 0)
            assertEquals(imageFile.name, drawings.first().fileName)
            val saved = BitmapFactory.decodeFile(File(context.filesDir, "drawings/${drawings.first().fileName}").path)!!
            assertEquals(20, saved.width)
            assertEquals(30, saved.height)
            assertEquals(Color.RED, saved.getPixel(0, 0))
            assertEquals(Color.BLUE, saved.getPixel(10, 15))
        } finally {
            database.close()
            context.deleteDatabase(name)
            imageFile.delete()
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
        val name = "drawing-delete-test-${System.currentTimeMillis()}"
        val imageFile = testImage()
        var database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()
        try {
            val deletedId = database.drawingDAO().insert(DrawingRecord(fileName = imageFile.name)).toInt()
            val keptId = database.drawingDAO().insert(DrawingRecord(fileName = imageFile.name)).toInt()
            database.drawingDAO().delete(DrawingRecord(id = deletedId, fileName = imageFile.name))
            database.close()
            database = Room.databaseBuilder(context, DrawingDatabase::class.java, name).build()

            assertEquals(listOf(keptId), database.drawingDAO().getAll().first().map { it.id })
        } finally {
            database.close()
            context.deleteDatabase(name)
            imageFile.delete()
        }
    }

    @Test
    fun repository_savesUpdatesAndDeletesImageFiles() = runBlocking {
        val repository = DrawingRepository.getRepository(context)
        val dao = DrawingDatabase.getDatabase(context).drawingDAO()
        val id = repository.add(Drawing(backing = bitmap())).toInt()
        try {
            val original = File(context.filesDir, "drawings/${dao.getById(id)!!.fileName}")
            assertTrue(original.exists())
            assertEquals(Color.BLUE, BitmapFactory.decodeFile(original.path)!!.getPixel(10, 15))

            val edited = bitmap().apply { eraseColor(Color.GREEN) }
            repository.update(Drawing(id, edited))
            val replacement = File(context.filesDir, "drawings/${dao.getById(id)!!.fileName}")
            assertFalse(original.exists())
            assertTrue(replacement.exists())
            assertEquals(Color.GREEN, repository.allDrawings.first().first { it.id == id }.backing.getPixel(10, 15))

            repository.remove(Drawing(id, edited))
            assertNull(dao.getById(id))
            assertFalse(replacement.exists())
        } finally {
            repository.allDrawings.first().filter { it.id == id }.forEach { repository.remove(it) }
        }
    }

    @Test
    fun migration_keepsOldDrawingAndMovesPngToFile() = runBlocking {
        val name = "drawing-migration-test-${System.currentTimeMillis()}"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { oldDatabase ->
            oldDatabase.execSQL("CREATE TABLE drawings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, backing BLOB NOT NULL)")
            val png = ByteArrayOutputStream()
            bitmap().compress(Bitmap.CompressFormat.PNG, 100, png)
            val values = ContentValues().apply {
                put("id", 7)
                put("backing", png.toByteArray())
            }
            oldDatabase.insertOrThrow("drawings", null, values)
            oldDatabase.version = 1
        }
        val database = Room.databaseBuilder(context, DrawingDatabase::class.java, name)
            .addMigrations(DrawingMigration(context)).build()
        var migratedFile: File? = null
        try {
            val record = database.drawingDAO().getAll().first().single()
            migratedFile = File(context.filesDir, "drawings/${record.fileName}")
            assertEquals(7, record.id)
            assertTrue(migratedFile.exists())
            val saved = BitmapFactory.decodeFile(migratedFile.path)!!
            assertEquals(20, saved.width)
            assertEquals(30, saved.height)
            assertEquals(Color.RED, saved.getPixel(0, 0))
            assertEquals(Color.BLUE, saved.getPixel(10, 15))
        } finally {
            database.close()
            context.deleteDatabase(name)
            migratedFile?.delete()
        }
    }

    @Test
    fun photosExport_writesReadablePng() = runBlocking {
        // Older Android versions request storage permission from the screen.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val uri = DrawingRepository.getRepository(context).saveToPhotos(bitmap())
        try {
            val saved = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }!!
            assertEquals(20, saved.width)
            assertEquals(30, saved.height)
            assertEquals(Color.BLUE, saved.getPixel(10, 15))
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    @Test
    fun sharedImage_hasContentUriAndCanBeImportedAgain() = runBlocking {
        val repository = DrawingRepository.getRepository(context)
        val uri = repository.prepareShare(bitmap())
        try {
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.fileprovider", uri.authority)
            assertEquals("image/png", context.contentResolver.getType(uri))
            val imported = repository.importImage(uri)
            assertEquals(20, imported.width)
            assertEquals(30, imported.height)
            assertEquals(Color.RED, imported.getPixel(0, 0))
            assertEquals(Color.BLUE, imported.getPixel(10, 15))
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    @Test
    fun importImage_rejectsInvalidImage() = runBlocking {
        val uri = DrawingRepository.getRepository(context).prepareShare(bitmap())
        try {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(byteArrayOf(1, 2, 3)) }
            var rejected = false
            try {
                DrawingRepository.getRepository(context).importImage(uri)
            } catch (e: IOException) {
                rejected = true
            }
            assertTrue(rejected)
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    @Test
    fun importImage_keepsOriginalDimensions() = runBlocking {
        val repository = DrawingRepository.getRepository(context)
        val large = Bitmap.createBitmap(4096, 1024, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val uri = repository.prepareShare(large)
        large.recycle()
        try {
            val imported = repository.importImage(uri)
            assertEquals(4096, imported.width)
            assertEquals(1024, imported.height)
            assertEquals(Color.RED, imported.getPixel(0, 0))
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }
}
