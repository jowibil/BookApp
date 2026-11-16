package com.bookappp.app.data.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.bookappp.app.di.BookDir
import com.bookappp.app.di.CoverDir
import com.bookappp.app.domain.model.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reasons an import can fail, surfaced to the UI for friendly error display. */
sealed class ImportResult {
    data class Success(val storedFileName: String, val format: BookFormat) : ImportResult()
    data class Failure(val error: ImportError) : ImportResult()
}

enum class ImportError {
    /** We couldn't read the picked content at all. */
    UNREADABLE,
    /** Not an EPUB or PDF (or unrecognized magic bytes). */
    UNSUPPORTED,
    /** Disk full or I/O error while copying. */
    STORAGE_FAILURE,
}

/**
 * Copies a SAF-picked file into app-private storage (AGENTS.md step 3).
 *
 * We copy rather than persisting the source URI permission: the book then lives in our
 * storage and cannot break if the user later deletes or moves the original file.
 */
@Singleton
class BookImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @BookDir private val bookDir: File,
    @CoverDir private val coverDir: File,
) {

    /** Reads the header for magic-byte format detection; buf is reused to keep a single allocation. */
    suspend fun import(sourceUri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val originalName = queryDisplayName(sourceUri)

        val header = try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val buf = ByteArray(HEADER_SIZE)
                var read = 0
                while (read < HEADER_SIZE) {
                    val n = input.read(buf, read, HEADER_SIZE - read)
                    if (n <= 0) break
                    read += n
                }
                buf.copyOf(read)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IOException) {
            null
        }

        if (header == null || header.isEmpty()) {
            return@withContext ImportResult.Failure(ImportError.UNREADABLE)
        }

        // Only accept by magic bytes; extension lies, bytes don't.
        val format = BookFileFormat.detectFromHeader(header)
            ?: return@withContext ImportResult.Failure(ImportError.UNSUPPORTED)

        val storedName = BookFileFormat.storageFileName(originalName, format)
        val target = uniqueTargetFile(File(bookDir, storedName))

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output ->
                    // Skip the already-read header? No: the header read above used its own
                    // stream instance, so copy the full original content here.
                    input.copyTo(output)
                }
            } ?: return@withContext ImportResult.Failure(ImportError.UNREADABLE)
        } catch (_: IOException) {
            target.delete()
            return@withContext ImportResult.Failure(ImportError.STORAGE_FAILURE)
        }

        ImportResult.Success(storedFileName = target.name, format = format)
    }

    private fun uniqueTargetFile(base: File): File {
        if (!base.exists()) return base
        val name = base.nameWithoutExtension
        val ext = base.extension
        var counter = 1
        var candidate = File(base.parentFile, "$name ($counter).$ext")
        while (candidate.exists()) {
            counter++
            candidate = File(base.parentFile, "$name ($counter).$ext")
        }
        return candidate
    }

    private fun queryDisplayName(uri: Uri): String = try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    } catch (_: SecurityException) {
        null
    } ?: FALLBACK_ORIGINAL_NAME

    private companion object {
        const val HEADER_SIZE = 16
        const val FALLBACK_ORIGINAL_NAME = "imported-book.epub"
    }
}
