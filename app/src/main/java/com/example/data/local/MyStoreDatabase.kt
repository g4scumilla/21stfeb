package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CachedAppEntity::class,
        CatalogMetaEntity::class,
        DownloadRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class MyStoreDatabase : RoomDatabase() {
    abstract fun myStoreDao(): MyStoreDao

    companion object {
        @Volatile
        private var INSTANCE: MyStoreDatabase? = null

        fun getInstance(context: Context): MyStoreDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MyStoreDatabase::class.java,
                    "my_store_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
