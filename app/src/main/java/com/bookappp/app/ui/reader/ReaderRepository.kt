package com.bookappp.app.ui.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.bookappp.app.data.db.BookDao
import com.bookappp.app.data.db.BookEntity
import com.bookappp.app.data.db.ReadingProgressDao
import com.bookappp.app.data.db.ReadingProgressEntity
import com.bookappp.app.data.files.BookMetadataExtractor
import com.bookappp.app.di.BookDir
import com.bookappp.app.di.CoverDir
import com.bookappp.app.domain.model.BookFormat
import com.bookappp.app.domain.model.SearchSnippet
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.abs
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** One in-book search match with display-ready context. */
data class SearchHit(
    val locator: Locator,
    val title: String,
    val snippet: SearchSnippet,
)

/** Cap on returned matches; the search sheet is a jump list, not a full report. */
private const val MAX_SEARCH_RESULTS = 60

/**
 * Opens publications with Readium and keeps them in memory for the process lifetime.
 * The app owns Publication transfer between screens: Readium's own Intent helpers
 * were removed in 3.0 (see the 3.0 migration guide).
 *
 * Readium glue stays isolated in ui/reader per AGENTS.md.
 */
@Singleton
class ReaderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @BookDir private val bookDir: File,
    @CoverDir private val coverDir: File,
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
) : BookMetadataExtractor {

    /** Publications currently open, keyed by book row id. */
    private val publications = ConcurrentHashMap<Long, Publication>()

    /** Serializes open attempts so the same book can't be parsed twice concurrently. */
    private val openMutex = Mutex()

    private val assetRetriever = AssetRetriever(
        contentResolver = context.contentResolver,
        httpClient = DefaultHttpClient(),
    )

    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context,
            httpClient = DefaultHttpClient(),
            assetRetriever = assetRetriever,
            // Parses PDF metadata, page count and cover through Readium's Pdfium adapter.
            pdfFactory = PdfiumDocumentFactory(context),
        ),
    )

    /**
     * Opens (or returns the cached) Publication for a book row. Parsing runs off the
     * main thread; AGENTS.md forbids blocking the UI thread while opening a book.
     *
     * Failure is reported as a Throwable so the UI can show a friendly message.
     */
    sealed interface OpenResult {
        data class Success(val publication: Publication, val format: BookFormat) : OpenResult
        data class Failure(val error: Throwable) : OpenResult
    }

    /** Where a book should resume, after checking the saved row against the publication. */
    sealed interface ReadingPosition {
        data class Restored(val locator: Locator) : ReadingPosition

        /** A row existed but points at a chapter this publication does not contain. */
        data object Discarded : ReadingPosition
        data object None : ReadingPosition
    }

    suspend fun openPublication(bookId: Long): OpenResult {
        publications[bookId]?.let { return OpenResult.Success(it, formatOf(it)) }

        openMutex.withLock {
            // Double-check under the lock: two coroutines may have raced here.
            publications[bookId]?.let { return OpenResult.Success(it, formatOf(it)) }

            return withContext(Dispatchers.IO) {
                val book = bookDao.getById(bookId)
                    ?: return@withContext OpenResult.Failure(
                        IllegalStateException("No book row for id $bookId"),
                    )
                val file = File(bookDir, book.fileName)
                if (!file.exists()) {
                    return@withContext OpenResult.Failure(
                        FileNotFoundException("Missing book file: ${book.fileName}"),
                    )
                }

                // Hint the sniffer with the stored format; it still checks the bytes.
                val hints = when (book.format) {
                    "EPUB" -> FormatHints().addFileExtension("epub")
                    "PDF" -> FormatHints().addFileExtension("pdf")
                    else -> FormatHints()
                }
                val asset: org.readium.r2.shared.util.asset.Asset =
                    when (val retrieved = assetRetriever.retrieve(file, hints)) {
                        is Try.Success -> retrieved.value
                        is Try.Failure -> {
                            return@withContext OpenResult.Failure(
                                IllegalStateException(
                                    "Asset retrieval failed: ${retrieved.value}",
                                ),
                            )
                        }
                    }

                val isEpub = asset.format.conformsTo(Specification.Epub)
                // Checked at media type level because the PDF profile also requires a
                // readable PDF engine, which the parser below decides.
                val isPdf = asset.format.mediaType == MediaType.PDF
                if (!isEpub && !isPdf) {
                    return@withContext OpenResult.Failure(
                        UnsupportedOperationException(
                            "Only DRM-free EPUB and PDF are readable (was ${asset.format}).",
                        ),
                    )
                }

                when (val opened = publicationOpener.open(asset, allowUserInteraction = false)) {
                    is Try.Success -> {
                        publications[bookId] = opened.value
                        OpenResult.Success(opened.value, formatOf(opened.value))
                    }
                    is Try.Failure -> OpenResult.Failure(
                        IllegalStateException("Publication opening failed: ${opened.value}"),
                    )
                }
            }
        }
    }

    fun publicationFor(bookId: Long): Publication? = publications[bookId]

    /** Library title fallback when a format omits its embedded title (common in PDFs). */
    suspend fun bookTitle(bookId: Long): String? = withContext(Dispatchers.IO) {
        bookDao.getById(bookId)?.title
    }

    /** Which reader renders this publication: PDF has its own navigator. */
    private fun formatOf(publication: Publication): BookFormat =
        if (publication.conformsTo(Publication.Profile.PDF)) BookFormat.PDF else BookFormat.EPUB

    /** Called when the reader is left, freeing the in-memory publication. */
    fun closePublication(bookId: Long) {
        publications.remove(bookId)
    }

    /**
     * Validates a newly imported file with the real parser and writes its metadata and
     * cover before the library row becomes visible. Always releases the publication so
     * importing does not keep a book in memory until it is read.
     */
    override suspend fun extractMetadata(bookId: Long): Boolean = try {
        when (openPublication(bookId)) {
            is OpenResult.Success -> {
                ensureMetadataExtracted(bookId)
                true
            }
            is OpenResult.Failure -> false
        }
    } finally {
        closePublication(bookId)
    }

    /**
     * Persists real metadata parsed from the publication the first time the book is
     * opened. Cheap to call repeatedly: it no-ops once title, author and cover exist.
     */
    suspend fun ensureMetadataExtracted(bookId: Long) = withContext(Dispatchers.IO) {
        val book = bookDao.getById(bookId) ?: return@withContext
        val publication = publications[bookId] ?: return@withContext

        val publicationTitle = publication.metadata.title ?: book.title
        val author = publication.metadata.authors.firstOrNull()?.name
        val alreadyHasRealTitle = !book.title.isFileNameLike(book.fileName)

        val needsCover = book.coverPath == null
        if (alreadyHasRealTitle && author == null && !needsCover) return@withContext

        // Cover rendering is a suspend call returning a Bitmap (CoverService).
        val coverBitmap: Bitmap? = runCatching { publication.cover() }.getOrNull()
        var coverPath: String? = book.coverPath
        if (needsCover && coverBitmap != null) {
            coverPath = saveCover(bookId, coverBitmap)
        }

        bookDao.update(
            book.copy(
                title = publicationTitle,
                author = author ?: book.author,
                coverPath = coverPath,
            ),
        )
    }

    private fun saveCover(bookId: Long, bitmap: Bitmap): String? {
        val coverFile = File(coverDir, "$bookId.png")
        // Covers are grid thumbnails, not archival images. Scaling here keeps the
        // database-adjacent files small and avoids decoding huge originals in the list.
        val scale = minOf(1f, COVER_MAX_WIDTH.toFloat() / bitmap.width.coerceAtLeast(1))
        val output = if (scale < 1f) {
            bitmap.scale(
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
            )
        } else {
            bitmap
        }
        return try {
            coverFile.outputStream().use { out ->
                output.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            coverFile.absolutePath
        } catch (_: Exception) {
            // Cover is optional decoration; never fail metadata extraction on it.
            null
        } finally {
            if (output !== bitmap) output.recycle()
        }
    }

    suspend fun updateLastOpened(bookId: Long) {
        bookDao.touchLastOpened(bookId, System.currentTimeMillis())
    }

    /** Persists the current locator (serialized to JSON per AGENTS.md data model). */
    suspend fun saveProgress(bookId: Long, locator: Locator) = withContext(Dispatchers.IO) {
        val json = locator.toJSON().toString()
        val existing = progressDao.getByBookId(bookId)
        val entity = ReadingProgressEntity(
            id = existing?.id ?: 0,
            bookId = bookId,
            locator = json,
            progression = locator.locations.totalProgression,
            updatedAt = System.currentTimeMillis(),
        )
        if (existing != null) progressDao.update(entity) else progressDao.upsertWithNewId(entity)
    }

    /**
     * Resolves a TOC HREF (stored as a string in the domain layer) to a full
     * Locator using Readium's locatorFromLink, so navigation carries titles and
     * progression along. Falls back to a bare-HREF locator.
     */
    suspend fun locatorForHref(bookId: Long, href: String): Locator? {
        val publication = publications[bookId] ?: return null
        // Nested TOC entries are common (parts → chapters); a top-level-only lookup
        // silently fell back to a bare locator and lost the chapter title.
        val link = findTocLink(publication.tableOfContents, href)
        // locatorFromLink is authoritative; the bare-HREF locator is the fallback when
        // the TOC entry is stale relative to the actual links.
        return link?.let { publication.locatorFromLink(it) } ?: bareLocator(publication, href)
    }

    /**
     * The next spine resource after [current], or null on the last one. Pull-to-advance
     * uses the reading order rather than the TOC because the TOC can skip front matter
     * and unlisted sections, while the spine is the actual order of the book.
     */
    fun nextReadingOrderLocator(bookId: Long, current: Locator): Locator? {
        val publication = publications[bookId] ?: return null
        val nextHref = nextReadingOrderHref(
            locatorHref = current.href.toString(),
            readingOrder = publication.readingOrder.map { it.href.toString() },
        ) ?: return null
        val link = publication.readingOrder.firstOrNull { it.href.toString() == nextHref }
            ?: return bareLocator(publication, nextHref)
        return publication.locatorFromLink(link) ?: bareLocator(publication, nextHref)
    }

    /** Cheap check so the last chapter does not offer a next-chapter pull. */
    fun hasNextReadingOrderResource(bookId: Long, currentHref: String): Boolean {
        val publication = publications[bookId] ?: return false
        return nextReadingOrderHref(
            locatorHref = currentHref,
            readingOrder = publication.readingOrder.map { it.href.toString() },
        ) != null
    }

    /**
     * The previous spine resource, opened at its last position so pulling down continues
     * from the end of the previous chapter instead of dropping the reader at its start.
     * Null on the first resource or when the locator is not in the reading order.
     */
    @OptIn(ExperimentalReadiumApi::class)
    suspend fun previousReadingOrderLocator(bookId: Long, current: Locator): Locator? {
        val publication = publications[bookId] ?: return null
        val previousHref = previousReadingOrderHref(
            locatorHref = current.href.toString(),
            readingOrder = publication.readingOrder.map { it.href.toString() },
        ) ?: return null

        return withContext(Dispatchers.IO) {
            val target = previousHref.substringBefore('#')
            val positions = runCatching { publication.positions() }.getOrDefault(emptyList())
            positions.lastOrNull { it.href.toString().substringBefore('#') == target }
                ?: run {
                    val link = publication.readingOrder.firstOrNull {
                        it.href.toString().substringBefore('#') == target
                    } ?: return@run null
                    val fallback = publication.locatorFromLink(link)
                        ?: bareLocator(publication, previousHref)
                        ?: return@run null
                    // No positions service: ask the navigator for the end of the resource.
                    fallback.copy(locations = fallback.locations.copy(progression = 1.0))
                }
        }
    }

    /** Cheap check so the first chapter does not offer a previous-chapter pull. */
    fun hasPreviousReadingOrderResource(bookId: Long, currentHref: String): Boolean {
        val publication = publications[bookId] ?: return false
        return previousReadingOrderHref(
            locatorHref = currentHref,
            readingOrder = publication.readingOrder.map { it.href.toString() },
        ) != null
    }

    /**
     * Resolves a percentage of the book to a locator through Readium's positions
     * service. This is the fallback for TOCs whose titles carry no chapter numbers,
     * and works for both EPUB and PDF.
     */
    @OptIn(ExperimentalReadiumApi::class)
    suspend fun locatorForProgression(bookId: Long, progression: Float): Locator? {
        val publication = publications[bookId] ?: return null
        val target = progression.coerceIn(0f, 1f).toDouble()
        return withContext(Dispatchers.IO) {
            val positions = runCatching { publication.positions() }.getOrNull()
            positions?.minByOrNull { locator ->
                abs((locator.locations.totalProgression ?: 0.0) - target)
            } ?: run {
                val order = publication.readingOrder
                if (order.isEmpty()) return@run null
                val index = (target * order.size).toInt().coerceIn(0, order.lastIndex)
                val link = order[index]
                publication.locatorFromLink(link) ?: bareLocator(publication, link.href.toString())
            }
        }
    }

    private fun findTocLink(links: List<Link>, href: String): Link? {
        for (link in links) {
            if (link.href.toString() == href) return link
            findTocLink(link.children, href)?.let { return it }
        }
        return null
    }

    private fun bareLocator(publication: Publication, href: String): Locator? {
        // Url.invoke(String) is Kotlin's operator; invalid input yields a Try-style null path.
        val url: org.readium.r2.shared.util.Url = runCatching {
            org.readium.r2.shared.util.Url(href)
        }.getOrNull() ?: return null
        // The media type decides how the navigator renders the target; a PDF chapter
        // must not be opened as if it were XHTML.
        val mediaType = publication.readingOrder
            .firstOrNull { it.href.toString().substringBefore('#') == href.substringBefore('#') }
            ?.mediaType
            ?: org.readium.r2.shared.util.mediatype.MediaType.XHTML
        // Non-null component types (MediaType/Locations/Text) get their empty defaults.
        return Locator(
            href = url,
            mediaType = mediaType,
            title = null,
            locations = org.readium.r2.shared.publication.Locator.Locations(emptyList(), null, null, null, emptyMap()),
            text = org.readium.r2.shared.publication.Locator.Text(null, null, null),
        )
    }

    /**
     * Runs an in-book search and returns up to [maxResults] matches.
     *
     * Returns null when there is nothing to search (publication not open, or no
     * SearchService) so the UI can say so instead of showing an empty result list.
     * Read errors stop the iteration and keep whatever was already found.
     */
    @OptIn(ExperimentalReadiumApi::class)
    suspend fun search(
        bookId: Long,
        query: String,
        maxResults: Int = MAX_SEARCH_RESULTS,
    ): List<SearchHit>? {
        val publication = publications[bookId] ?: return null
        return withContext(Dispatchers.IO) {
            val iterator = publication.search(query) ?: return@withContext null
            val hits = mutableListOf<SearchHit>()
            try {
                while (hits.size < maxResults) {
                    val page = when (val result = iterator.next()) {
                        is Try.Success -> result.value ?: break
                        is Try.Failure -> break
                    }
                    // A page without locators would spin this loop forever.
                    if (page.locators.isEmpty()) break
                    for (locator in page.locators) {
                        hits += SearchHit(
                            locator = locator,
                            title = locator.title.orEmpty(),
                            snippet = SearchSnippet.from(
                                before = locator.text.before,
                                match = locator.text.highlight,
                                after = locator.text.after,
                            ),
                        )
                        if (hits.size >= maxResults) break
                    }
                }
            } finally {
                iterator.close()
            }
            hits
        }
    }

    /**
     * Looks up where [bookId] should resume. A stored locator is only applied when its href
     * is still a spine resource of this publication: a position left over from another
     * book, or from a chapter the file no longer has, would otherwise open a blank page
     * with no way back. Invalid rows are deleted so later opens start cleanly instead of
     * retrying a position that can never render.
     */
    suspend fun readingPosition(bookId: Long): ReadingPosition {
        val row = progressDao.getByBookId(bookId) ?: return ReadingPosition.None
        val locator = row.locator
            ?.let { json -> runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull() }
        val publication = publications[bookId]
        if (locator != null && publication != null) {
            val readingOrder = publication.readingOrder.map { it.href.toString() }
            if (isLocatorInReadingOrder(locator.href.toString(), readingOrder)) {
                return ReadingPosition.Restored(locator)
            }
        }
        progressDao.deleteByBookId(bookId)
        return ReadingPosition.Discarded
    }

    /** True when a title still equals the imported file name without extension. */
    private fun String.isFileNameLike(fileName: String): Boolean {
        val base = fileName.substringBeforeLast('.')
        return equals(base, ignoreCase = true)
    }
}

/** Grid covers are rendered at card size; 600 px is ample for a 2x tablet cell. */
private const val COVER_MAX_WIDTH = 600

/**
 * True when [locatorHref] names a document in [readingOrder]. Fragments are ignored: a
 * position inside a chapter (`chapter.xhtml#part-2`) still points at that chapter.
 */
internal fun isLocatorInReadingOrder(locatorHref: String, readingOrder: List<String>): Boolean {
    val target = locatorHref.substringBefore('#')
    return target.isNotBlank() && readingOrder.any { it.substringBefore('#') == target }
}

/**
 * The next href in [readingOrder] after [locatorHref], or null when it is the last
 * resource. Fragments are ignored, so a locator inside a chapter still resolves.
 */
internal fun nextReadingOrderHref(locatorHref: String, readingOrder: List<String>): String? {
    val current = locatorHref.substringBefore('#')
    if (current.isBlank()) return null
    val index = readingOrder.indexOfFirst { it.substringBefore('#') == current }
    if (index < 0) return null
    return readingOrder.getOrNull(index + 1)
}

/**
 * The previous href in [readingOrder], or null on the first resource. Fragments are
 * ignored, so a locator inside a chapter still resolves its predecessor.
 */
internal fun previousReadingOrderHref(locatorHref: String, readingOrder: List<String>): String? {
    val current = locatorHref.substringBefore('#')
    if (current.isBlank()) return null
    val index = readingOrder.indexOfFirst { it.substringBefore('#') == current }
    if (index <= 0) return null
    return readingOrder[index - 1]
}
