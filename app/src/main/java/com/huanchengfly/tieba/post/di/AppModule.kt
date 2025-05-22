package com.huanchengfly.tieba.post.di

import android.content.Context
import androidx.room.Room
import com.huanchengfly.tieba.post.models.database.AccountDao
import com.huanchengfly.tieba.post.models.database.AppDatabase
import com.huanchengfly.tieba.post.models.database.DraftDao
import com.huanchengfly.tieba.post.models.database.HistoryDao
import com.huanchengfly.tieba.post.models.database.HistoryRepository
import com.huanchengfly.tieba.post.models.database.SearchPostHistoryDao
import com.huanchengfly.tieba.post.models.database.TopForumDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideHistoryRepository(dao: HistoryDao): HistoryRepository = HistoryRepository(dao)

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideSearchPostHistoryDao(db: AppDatabase): SearchPostHistoryDao =
        db.searchPostHistoryDao()


    @Provides
    @Singleton
    fun provideDraftDao(db: AppDatabase): DraftDao = db.draftDao()

    @Provides
    @Singleton
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    @Singleton
    fun provideTopForumDao(db: AppDatabase): TopForumDao = db.topForumDao()

    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "your_db_name").build()

}