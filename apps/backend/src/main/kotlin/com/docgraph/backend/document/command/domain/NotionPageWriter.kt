package com.docgraph.backend.document.command.domain

import java.time.OffsetDateTime

fun interface NotionPageWriter {
    fun patch(
        notionBlockId: String,
        newText: String,
        expectedLastEditedAt: OffsetDateTime?,
    ): NotionPatchResult
}
