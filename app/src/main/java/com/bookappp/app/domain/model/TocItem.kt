package com.bookappp.app.domain.model

/**
 * Flattened table-of-contents entry for list UIs.
 * Pure domain: no Readium types, so it can be unit tested on the JVM directly.
 */
data class TocItem(
    val href: String,
    val title: String,
    /** Depth in the original tree, 0 for top-level entries. */
    val depth: Int,
) {
    companion object {
        /**
         * Depth-first flattening of a generic tree whose nodes expose title,
         * href and children. Pure and recursive — JVM-testable.
         */
        fun fromTree(
            roots: List<TocNode>,
            maxDepth: Int = DEFAULT_MAX_DEPTH,
        ): List<TocItem> {
            val out = mutableListOf<TocItem>()
            fun walk(nodes: List<TocNode>, depth: Int) {
                for (node in nodes) {
                    if (node.href.isNotBlank()) {
                        out += TocItem(
                            href = node.href,
                            title = node.title.ifBlank { node.href },
                            depth = depth,
                        )
                        // Stop descending once the cap is reached; deeper levels
                        // are neither emitted nor traversed.
                        if (depth >= maxDepth) continue
                    }
                    if (node.children.isNotEmpty()) walk(node.children, depth + 1)
                }
            }
            walk(roots, 0)
            return out
        }

        private const val DEFAULT_MAX_DEPTH = 6
    }
}

/** Simple tree node the mapper fills from Readium's Links. */
data class TocNode(
    val title: String,
    val href: String,
    val children: List<TocNode> = emptyList(),
)
