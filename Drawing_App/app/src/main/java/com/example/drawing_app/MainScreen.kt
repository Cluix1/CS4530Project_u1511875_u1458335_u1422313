package com.example.drawing_app
import android.app.Application
import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Room
import androidx.room3.Update
import androidx.room3.Dao
import androidx.room3.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// actual db operations
@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings ORDER BY id DESC")
    fun getAll(): Flow<List<DrawingRecord>>

    @Query("SELECT * FROM drawings WHERE id = :id")
    suspend fun getById(id: Int): DrawingRecord?

    @Insert
    suspend fun insert(d: DrawingRecord): Long

    @Update
    suspend fun update(d: DrawingRecord): Int

    @Delete
    suspend fun delete(d: DrawingRecord)
}

// Singleton
@Database(entities = [DrawingRecord::class], version = 2, exportSchema = false)
abstract class DrawingDatabase : RoomDatabase() {
    abstract fun drawingDAO(): DrawingDao

    companion object {
        @Volatile
        private var INSTANCE: DrawingDatabase? = null

        fun getDatabase(context: Context): DrawingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DrawingDatabase::class.java,
                    "drawing_database"
                ).addMigrations(DrawingMigration(context)).build().also { INSTANCE = it }
            }
        }
    }
}

class DrawingRepository private constructor(private val context: Context, private val dao: DrawingDao) {
    private val directory = File(context.filesDir, "drawings")

    val allDrawings: Flow<List<Drawing>> = dao.getAll().map { records ->
        records.mapNotNull { record ->
            val bitmap = BitmapFactory.decodeFile(File(directory, record.fileName).path)
            if (bitmap == null) null else Drawing(record.id, bitmap)
        }
    }.flowOn(Dispatchers.IO)

    private fun saveImage(bitmap: Bitmap): File {
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Could not create drawings folder")
        val file = File(directory, "Drawing_${UUID.randomUUID()}.png")
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw IOException("Could not save image")
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        return file
    }

    suspend fun add(d: Drawing): Long = withContext(Dispatchers.IO) {
        val file = saveImage(d.backing)
        try {
            dao.insert(DrawingRecord(id = d.id, fileName = file.name))
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    suspend fun update(d: Drawing) = withContext(Dispatchers.IO) {
        val previous = dao.getById(d.id) ?: throw IOException("Drawing no longer exists")
        val file = saveImage(d.backing)
        try {
            if (dao.update(DrawingRecord(d.id, file.name)) == 0) throw IOException("Drawing no longer exists")
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        File(directory, previous.fileName).delete()
        Unit
    }

    suspend fun remove(d: Drawing) = withContext(Dispatchers.IO) {
        val record = dao.getById(d.id) ?: return@withContext
        dao.delete(record)
        File(directory, record.fileName).delete()
        Unit
    }

    suspend fun importImage(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
            ?: throw IOException("Could not read image")
    }

    suspend fun prepareShare(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "shared_drawings")
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Could not create share folder")
        val file = File(directory, "Drawing_${UUID.randomUUID()}.png")
        try {
            file.outputStream().use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Could not share image")
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    suspend fun saveToPhotos(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Drawing_${UUID.randomUUID()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DrawingApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create the image")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("Could not open the image")
            output.use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                    throw IOException("Could not save the image")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        uri
    }

    companion object {
        @Volatile
        private var INSTANCE: DrawingRepository? = null

        fun getRepository(context: Context): DrawingRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DrawingRepository(
                    context.applicationContext,
                    DrawingDatabase.getDatabase(context).drawingDAO()
                ).also { INSTANCE = it }
            }
        }
    }
}

// Created once per app launch
class DrawingApplication : Application() {
    val database by lazy { DrawingDatabase.getDatabase(this) }
    val repository by lazy { DrawingRepository.getRepository(this) }

}

