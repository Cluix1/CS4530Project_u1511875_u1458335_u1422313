package com.example.drawing_app
import android.app.Application
import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.util.UUID
import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import java.io.ByteArrayOutputStream
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Room
import androidx.room3.Update
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// actual db operations
@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings")
    fun getAll(): Flow<List<Drawing>>

    @Insert
    suspend fun insert(d: Drawing): Long

    @Update
    suspend fun update(d: Drawing)

    @Delete
    suspend fun delete(d: Drawing)
}

// Room can't store a Bitmap directly, so save it as PNG bytes
class BitmapConverter {
    @ColumnTypeConverter
    fun fromBitmap(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    @ColumnTypeConverter
    fun toBitmap(bytes: ByteArray): Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// Singleton
@Database(entities = [Drawing::class], version = 1)
@ColumnTypeConverters(BitmapConverter::class)
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
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class DrawingRepository private constructor(private val context: Context, private val dao: DrawingDao) {
    val allDrawings: Flow<List<Drawing>> = dao.getAll()

    suspend fun add(d: Drawing) = dao.insert(d)
    suspend fun update(d: Drawing) = dao.update(d)
    suspend fun remove(d: Drawing) = dao.delete(d)

    suspend fun saveToGallery(bitmap: Bitmap) = withContext(Dispatchers.IO) {
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

