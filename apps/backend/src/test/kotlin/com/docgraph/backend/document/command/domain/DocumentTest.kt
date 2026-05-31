package com.docgraph.backend.document.command.domain

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.IconType
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("unit")
class DocumentTest {

    @Test
    fun `refreshSnapshot updates Notion page snapshot fields`() {
        val document = Document(
            projectId = 1L,
            notionPageId = "page-1",
            parentNotionPageId = "parent-before",
            title = "Before",
            type = DocumentType.PLANNING,
            assigneeMemberId = 10L,
            rawContent = """{"old":true}""",
            flatText = "old text",
            notionCreatedBy = "user-before",
            notionLastEditedBy = "editor-before",
            notionLastEditedAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        )
        val previousSyncedAt = document.syncedAt
        val previousUpdatedAt = document.updatedAt
        val notionEditedAt = OffsetDateTime.parse("2026-01-02T00:00:00Z")

        document.refreshSnapshot(
            title = "After",
            parentNotionPageId = "parent-after",
            type = DocumentType.REQUIREMENTS,
            iconType = IconType.EMOJI,
            iconValue = "📄",
            assigneeMemberId = 20L,
            rawContent = """{"new":true}""",
            flatText = "new text",
            notionCreatedBy = "user-after",
            notionLastEditedBy = "editor-after",
            notionLastEditedAt = notionEditedAt,
        )

        assertEquals("After", document.title)
        assertEquals("parent-after", document.parentNotionPageId)
        assertEquals(DocumentType.REQUIREMENTS, document.type)
        assertEquals(IconType.EMOJI, document.iconType)
        assertEquals("📄", document.iconValue)
        assertEquals(20L, document.assigneeMemberId)
        assertEquals("""{"new":true}""", document.rawContent)
        assertEquals("new text", document.flatText)
        assertEquals("user-after", document.notionCreatedBy)
        assertEquals("editor-after", document.notionLastEditedBy)
        assertEquals(notionEditedAt, document.notionLastEditedAt)
        assertNotNull(document.syncedAt)
        assertNotNull(document.updatedAt)
        kotlin.test.assertTrue(document.syncedAt >= previousSyncedAt)
        kotlin.test.assertTrue(document.updatedAt >= previousUpdatedAt)
    }
}
