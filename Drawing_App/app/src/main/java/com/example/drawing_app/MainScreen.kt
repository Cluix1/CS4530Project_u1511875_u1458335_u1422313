package com.example.drawing_app
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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


// actual db operations
@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings")
    fun getAll(): Flow<List<Drawing>>

    @Insert
    suspend fun insert(d: Drawing)

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

class DrawingRepository(private val dao: DrawingDao) {
    val allDrawings: Flow<List<Drawing>> = dao.getAll()

    suspend fun add(d: Drawing) = dao.insert(d)
    suspend fun update(d: Drawing) = dao.update(d)
    suspend fun remove(d: Drawing) = dao.delete(d)
}

// Created once per app launch
class DrawingApplication : Application() {
    val database by lazy { DrawingDatabase.getDatabase(this) }
    val repository by lazy { DrawingRepository(database.drawingDAO()) }

}

