package com.docgraph.backend.notification.command.application

import com.docgraph.backend.document.query.application.DocumentReference
import com.docgraph.backend.document.query.application.SearchDocumentReferencesByIdsQuery
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.notification.command.domain.ProjectWebhook
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.notification.command.domain.WebhookNotifier
import com.docgraph.backend.project.query.application.ProjectRef
import com.docgraph.backend.project.query.application.SearchProjectRefsByIdsQuery
import com.docgraph.backend.validation.query.application.FindConflictTitleByIdQuery
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertTrue

@Tag("unit")
class ConflictNotificationDispatcherTest {

    private val projectWebhookRepository = mockk<ProjectWebhookRepository>()
    private val findEdgeById = mockk<FindEdgeByIdQuery>()
    private val searchProjectRefs = mockk<SearchProjectRefsByIdsQuery>()
    private val searchDocumentReferences = mockk<SearchDocumentReferencesByIdsQuery>()
    private val findConflictTitle = mockk<FindConflictTitleByIdQuery>()
    private val webhookNotifier = mockk<WebhookNotifier>()
    private val dispatcher = ConflictNotificationDispatcher(
        projectWebhookRepository,
        findEdgeById,
        searchProjectRefs,
        searchDocumentReferences,
        findConflictTitle,
        webhookNotifier,
    )

    @Test
    fun `webhook 설정됨 — 프로젝트명·source·target 문서명·title 포함 메시지 발송`() {
        every { findEdgeById.find(70L) } returns EdgeDetail(70L, 5L, 100L, 200L, "기준")
        every { projectWebhookRepository.findByProjectId(5L) } returns
            ProjectWebhook(projectId = 5L, url = "https://hooks.slack.com/x", createdAt = OffsetDateTime.now())
        every { searchProjectRefs.search(listOf(5L)) } returns listOf(ProjectRef(5L, 1L, "기획 프로젝트"))
        every { searchDocumentReferences.search(listOf(100L, 200L)) } returns listOf(
            DocumentReference(100L, 5L, "회의록", null, null),
            DocumentReference(200L, 5L, "요구사항", null, null),
        )
        every { findConflictTitle.find(10L) } returns "결정사항 미반영"
        val message = slot<String>()
        every { webhookNotifier.send("https://hooks.slack.com/x", capture(message)) } just Runs

        dispatcher.dispatch(conflictId = 10L, edgeId = 70L)

        verify(exactly = 1) { webhookNotifier.send("https://hooks.slack.com/x", any()) }
        assertTrue(message.captured.contains("기획 프로젝트"))
        assertTrue(message.captured.contains("회의록"))
        assertTrue(message.captured.contains("요구사항"))
        assertTrue(message.captured.contains("결정사항 미반영"))
    }

    @Test
    fun `webhook 미설정 — 발송·이후 cross-domain 조회 모두 skip`() {
        every { findEdgeById.find(70L) } returns EdgeDetail(70L, 5L, 100L, 200L, "기준")
        every { projectWebhookRepository.findByProjectId(5L) } returns null

        dispatcher.dispatch(conflictId = 10L, edgeId = 70L)

        verify(exactly = 0) { webhookNotifier.send(any(), any()) }
        verify(exactly = 0) { searchProjectRefs.search(any()) }
        verify(exactly = 0) { searchDocumentReferences.search(any()) }
        verify(exactly = 0) { findConflictTitle.find(any()) }
    }

    @Test
    fun `edge 없음 — skip, webhook 조회도 안 함`() {
        every { findEdgeById.find(70L) } returns null

        dispatcher.dispatch(conflictId = 10L, edgeId = 70L)

        verify(exactly = 0) { projectWebhookRepository.findByProjectId(any()) }
        verify(exactly = 0) { webhookNotifier.send(any(), any()) }
    }
}
