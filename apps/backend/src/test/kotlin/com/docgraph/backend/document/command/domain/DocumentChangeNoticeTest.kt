package com.docgraph.backend.document.command.domain

import com.docgraph.backend.event.OutboxStatus
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("unit")
class DocumentChangeNoticeTest {

    @Test
    fun `new notice starts as pending with zero attempts`() {
        val notice = notice()

        assertEquals(OutboxStatus.PENDING, notice.status)
        assertEquals(0, notice.attempts)
        assertNull(notice.lastAttemptAt)
        assertNull(notice.failureReason)
    }

    @Test
    fun `recordAttempt increments attempts and records lastAttemptAt`() {
        val notice = notice()

        notice.recordAttempt()

        assertEquals(1, notice.attempts)
        assertNotNull(notice.lastAttemptAt)

        notice.recordAttempt()

        assertEquals(2, notice.attempts)
    }

    @Test
    fun `markSuccess records success and clears failureReason`() {
        val notice = notice(failureReason = "temporary")

        notice.markSuccess()

        assertEquals(OutboxStatus.SUCCESS, notice.status)
        assertNull(notice.failureReason)
    }

    @Test
    fun `markFailed records failed status and reason`() {
        val notice = notice()

        notice.markFailed("Notion API failed")

        assertEquals(OutboxStatus.FAILED, notice.status)
        assertEquals("Notion API failed", notice.failureReason)
    }

    private fun notice(
        failureReason: String? = null,
    ): DocumentChangeNotice =
        DocumentChangeNotice(
            changeKind = DocumentChangeKind.CONTENT_UPDATED,
            projectId = 1L,
            notionEventId = "event-1",
            eventType = "page.content_updated",
            notionWorkspaceId = "workspace-1",
            subscriptionId = "subscription-1",
            notionPageId = "page-1",
            occurredAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
            rawPayload = """{"type":"page.content_updated"}""",
            failureReason = failureReason,
        )
}
