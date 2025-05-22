package com.huanchengfly.tieba.post.models.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Account::class,
        Draft::class,
        History::class,
        Block::class,
        TopForum::class,
        SearchHistory::class,
        SearchPostHistory::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun draftDao(): DraftDao
    abstract fun historyDao(): HistoryDao
    abstract fun blockDao(): BlockDao
    abstract fun topForumDao(): TopForumDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun searchPostHistoryDao(): SearchPostHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}