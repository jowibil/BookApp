package com.bookappp.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TocItemTest {

    @Test
    fun `flattens depth-first preserving order and depth`() {
        val tree = listOf(
            TocNode("Ch 1", "c1.xhtml", children = listOf(TocNode("1.1", "c1s1.xhtml"))),
            TocNode("Ch 2", "c2.xhtml"),
        )

        assertEquals(
            listOf(
                TocItem("c1.xhtml", "Ch 1", depth = 0),
                TocItem("c1s1.xhtml", "1.1", depth = 1),
                TocItem("c2.xhtml", "Ch 2", depth = 0),
            ),
            TocItem.fromTree(tree),
        )
    }

    @Test
    fun `blank titles fall back to href`() {
        val tree = listOf(TocNode("", "only-href.xhtml"))
        assertEquals(listOf(TocItem("only-href.xhtml", "only-href.xhtml", depth = 0)), TocItem.fromTree(tree))
    }

    @Test
    fun `entries without href are skipped but children kept`() {
        val tree = listOf(
            // Structural node with no target of its own (e.g. a parts wrapper).
            TocNode("Part I", "", children = listOf(TocNode("A", "a.xhtml"))),
        )
        assertEquals(listOf(TocItem("a.xhtml", "A", depth = 1)), TocItem.fromTree(tree))
    }

    @Test
    fun `max depth caps runaway trees`() {
        fun chain(depth: Int): TocNode =
            if (depth == 0) TocNode("leaf", "leaf.xhtml")
            else TocNode("d$depth", "d$depth.xhtml", children = listOf(chain(depth - 1)))

        val items = TocItem.fromTree(listOf(chain(10)), maxDepth = 3)
        // Depth 0..3 are emitted (maxDepth inclusive); nothing below d7 is traversed.
        assertEquals(4, items.size)
        assertEquals(0, items.first().depth)
        assertEquals(3, items.last().depth)
        assertEquals("d7.xhtml", items.last().href)
    }

    @Test
    fun `empty tree yields empty list`() {
        assertEquals(emptyList<TocItem>(), TocItem.fromTree(emptyList()))
    }
}
