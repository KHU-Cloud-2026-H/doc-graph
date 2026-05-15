package com.docgraph.backend.document.command.domain

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class BlockTest {

    @Test
    fun `refreshSnapshot updates Notion block snapshot fields`() {
        val document = Document(projectId = 1L, notionPageId = "page-1", title = "Page")
        val block = Block(
            document = document,
            notionBlockId = "block-1",
            parentType = "page_id",
            parentId = "page-1",
            type = "paragraph",
            text = "old text",
            sortOrder = 1,
            createdTime = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
            lastEditedTime = OffsetDateTime.parse("2026-01-01T01:00:00Z"),
            hasChildren = false,
            archived = false,
            inTrash = false,
            rawBlock = """{"type":"paragraph"}""",
        )
        val createdTime = OffsetDateTime.parse("2026-01-02T00:00:00Z")
        val lastEditedTime = OffsetDateTime.parse("2026-01-02T01:00:00Z")

        block.refreshSnapshot(
            parentType = "block_id",
            parentId = "parent-block",
            type = "toggle",
            text = "new text",
            sortOrder = 2,
            createdTime = createdTime,
            createdBy = "user-created",
            lastEditedTime = lastEditedTime,
            lastEditedBy = "user-edited",
            hasChildren = true,
            archived = true,
            inTrash = true,
            rawBlock = """{"type":"toggle"}""",
        )

        assertEquals("block_id", block.parentType)
        assertEquals("parent-block", block.parentId)
        assertEquals("toggle", block.type)
        assertEquals("new text", block.text)
        assertEquals(2, block.sortOrder)
        assertEquals(createdTime, block.createdTime)
        assertEquals("user-created", block.createdBy)
        assertEquals(lastEditedTime, block.lastEditedTime)
        assertEquals("user-edited", block.lastEditedBy)
        assertTrue(block.hasChildren)
        assertTrue(block.archived)
        assertTrue(block.inTrash)
        assertEquals("""{"type":"toggle"}""", block.rawBlock)
    }

    @Test
    fun `defaults optional Notion status flags to false`() {
        val document = Document(projectId = 1L, notionPageId = "page-1", title = "Page")
        val block = Block(
            document = document,
            notionBlockId = "block-1",
            type = "paragraph",
            sortOrder = 1,
        )

        assertFalse(block.hasChildren)
        assertFalse(block.archived)
        assertFalse(block.inTrash)
    }
}
