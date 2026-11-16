package com.bookappp.app.di

import android.content.Context
import androidx.room.Room
import com.bookappp.app.data.db.AppDatabase
import com.bookappp.app.data.db.BookDao
import com.bookappp.app.data.db.BookmarkDao
import com.bookappp.app.data.db.HighlightDao
import com.bookappp.app.data.db.ReadingProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides fun provideReadingProgressDao(db: AppDatabase): ReadingProgressDao = db.readingProgressDao()

    @Provides fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides fun provideHighlightDao(db: AppDatabase): HighlightDao = db.highlightDao()
}
