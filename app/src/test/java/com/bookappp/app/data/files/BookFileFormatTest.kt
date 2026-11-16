package com.bookappp.app.data.files

import com.bookappp.app.domain.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookFileFormatTest {

    private val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // EPUB container
    private val pdfHeader = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37)

    @Test
    fun `detects EPUB from ZIP magic bytes`() {
        assertEquals(BookFormat.EPUB, BookFileFormat.detectFromHeader(zipHeader))
    }

    @Test
    fun `detects PDF from header`() {
        assertEquals(BookFormat.PDF, BookFileFormat.detectFromHeader(pdfHeader))
    }

    @Test
    fun `unknown bytes are rejected`() {
        assertNull(BookFileFormat.detectFromHeader(byteArrayOf(0x00, 0x01, 0x02)))
    }

    @Test
    fun `empty header is rejected`() {
        assertNull(BookFileFormat.detectFromHeader(ByteArray(0)))
    }

    @Test
    fun `sanitizes path traversal components`() {
        assertEquals("story.epub", BookFileFormat.sanitizeFileName("../../downloads/story.epub"))
        assertEquals("story.epub", BookFileFormat.sanitizeFileName("C:\\Users\\me\\story.epub"))
    }

    @Test
    fun `empty after sanitize falls back`() {
        assertEquals("book", BookFileFormat.sanitizeFileName("...\\/:/"))
    }

    @Test
    fun `storage name keeps correct extension`() {
        val name = BookFileFormat.storageFileName("My Book.EPUB", BookFormat.EPUB)
        assertEquals("My Book.EPUB", name)
    }

    @Test
    fun `storage name fixes wrong extension`() {
        val name = BookFileFormat.storageFileName("My Book.txt", BookFormat.PDF)
        assertEquals("My Book.pdf", name)
    }

    @Test
    fun `storage name adds extension when missing`() {
        val name = BookFileFormat.storageFileName("novel", BookFormat.EPUB)
        assertEquals("novel.epub", name)
    }
}
