package com.bookappp.app.di

import android.content.Context
import com.bookappp.app.data.files.BookMetadataExtractor
import com.bookappp.app.data.repository.SettingsRepository
import com.bookappp.app.ui.reader.ReaderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // SettingsRepository owns the process-wide DataStore instance; one singleton.
    @Provides
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    // The reader owns the only Readium publication cache; the library borrows it once
    // at import to validate the file and persist metadata without a second parser.
    @Provides
    fun provideBookMetadataExtractor(readerRepository: ReaderRepository): BookMetadataExtractor =
        readerRepository
}
