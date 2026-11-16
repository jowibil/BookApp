package com.bookappp.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier

/** Name of the app-private directory holding imported book files. */
const val BOOK_DIR_NAME = "books"

/** Name of the app-private directory holding extracted cover thumbnails. */
const val COVER_DIR_NAME = "covers"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BookDir

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoverDir

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @BookDir
    fun provideBookDir(@ApplicationContext context: Context): File =
        File(context.filesDir, BOOK_DIR_NAME).apply { mkdirs() }

    @Provides
    @CoverDir
    fun provideCoverDir(@ApplicationContext context: Context): File =
        File(context.filesDir, COVER_DIR_NAME).apply { mkdirs() }
}
