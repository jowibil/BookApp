package com.bookappp.app.ui.legal

/** One renderable piece of a bundled legal document. */
sealed interface LegalBlock {
    data class Heading(val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
}

/**
 * Parses the plain-text documents in `res/raw` into headings and paragraphs.
 *
 * The format is deliberately tiny so the legal text stays readable in a text editor:
 * a line starting with "# " starts a heading, a blank line ends a block, and line
 * breaks inside a block are soft wraps that get joined.
 */
object LegalDocumentParser {
    private const val HEADING_PREFIX = "# "
    private val WHITESPACE = Regex("\\s+")

    fun parse(document: String): List<LegalBlock> {
        val blocks = mutableListOf<LegalBlock>()
        val buffer = StringBuilder()

        fun flush() {
            val text = buffer.toString().trim()
            buffer.clear()
            if (text.isEmpty()) return
            blocks += if (text.startsWith(HEADING_PREFIX)) {
                LegalBlock.Heading(text.removePrefix(HEADING_PREFIX).trim())
            } else {
                LegalBlock.Paragraph(text.replace(WHITESPACE, " "))
            }
        }

        // lines() splits on \n and \r\n alike, so the bundled text can be edited on any OS.
        document.lines().forEach { line ->
            if (line.isBlank()) flush() else buffer.appendLine(line.trim())
        }
        flush()
        return blocks
    }
}
