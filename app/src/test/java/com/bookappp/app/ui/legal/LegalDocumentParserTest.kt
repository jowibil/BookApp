package com.bookappp.app.ui.legal

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalDocumentParserTest {

    @Test
    fun `splits headings and paragraphs on blank lines`() {
        val document = """
            # Title

            First paragraph.

            # Section

            Second paragraph.
        """.trimIndent()

        assertEquals(
            listOf(
                LegalBlock.Heading("Title"),
                LegalBlock.Paragraph("First paragraph."),
                LegalBlock.Heading("Section"),
                LegalBlock.Paragraph("Second paragraph."),
            ),
            LegalDocumentParser.parse(document),
        )
    }

    @Test
    fun `joins hard-wrapped lines inside a paragraph`() {
        val document = "A paragraph that was\nwrapped in a text editor."

        assertEquals(
            listOf(LegalBlock.Paragraph("A paragraph that was wrapped in a text editor.")),
            LegalDocumentParser.parse(document),
        )
    }

    @Test
    fun `handles windows line endings and surrounding blank lines`() {
        val document = "\r\n# Title\r\n\r\nBody text.\r\n\r\n"

        assertEquals(
            listOf(LegalBlock.Heading("Title"), LegalBlock.Paragraph("Body text.")),
            LegalDocumentParser.parse(document),
        )
    }

    @Test
    fun `ignores an empty document`() {
        assertEquals(emptyList<LegalBlock>(), LegalDocumentParser.parse(""))
        assertEquals(emptyList<LegalBlock>(), LegalDocumentParser.parse("\n\n   \n"))
    }
}
