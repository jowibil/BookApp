package com.bookappp.app.ui.reader

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * EntryPoint for the singleton ReaderRepository so code paths outside Hilt's
 * member-injection graph (e.g. FragmentFactory restoration) can reach it synchronously.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReaderRepositoryEntryPoint {
    fun readerRepository(): ReaderRepository
}

fun readerRepositoryFor(context: Context): ReaderRepository =
    EntryPointAccessors.fromApplication(context, ReaderRepositoryEntryPoint::class.java)
        .readerRepository()
