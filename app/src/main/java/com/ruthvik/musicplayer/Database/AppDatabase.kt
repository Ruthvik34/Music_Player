package com.ruthvik.musicplayer.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ruthvik.musicplayer.Dao.PrivatePlayListDao
import com.ruthvik.musicplayer.Models.PrivatePlayListSongs

@Database(entities = [PrivatePlayListSongs::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): PrivatePlayListDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_player_database"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
