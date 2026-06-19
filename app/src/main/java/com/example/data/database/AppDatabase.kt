package com.example.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalTableEntity::class,
        LocalMenuItemEntity::class,
        LocalCategoryEntity::class,
        LocalOrderEntity::class,
        LocalKotEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDatabaseDao(): LocalDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d("DEBUG_APP", "ROOM_DB_VERSION: 2")
            return INSTANCE ?: synchronized(this) {
                val dbName = "captain_pos_local_db"
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                } catch (e: Exception) {
                    Log.d("DEBUG_APP", "ROOM_DB_ERROR: ${e.message}")
                    context.deleteDatabase(dbName)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                }
                
                Log.d("DEBUG_APP", "ROOM_DB_INITIALIZED")
                INSTANCE = instance
                instance
            }
        }
    }
}
