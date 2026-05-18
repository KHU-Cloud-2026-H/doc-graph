package com.docgraph.backend.workspace.command.interfaces.event

import com.docgraph.backend.auth.command.domain.NotionWorkspaceAuthorizedEvent
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("component")
@SpringBootTest
@Import(SharedPostgresContainer::class)
class NotionWorkspaceAuthorizedEventListenerTest @Autowired constructor(
    private val publisher: ApplicationEventPublisher,
    private val workspaceRepository: WorkspaceRepository,
) {

    @Test
    fun `event publish — workspace row 생성`() {
        val notionWorkspaceId = "nw-listener-${System.nanoTime()}"
        publisher.publishEvent(
            NotionWorkspaceAuthorizedEvent(
                ownerUserId = 1L,
                notionWorkspaceId = notionWorkspaceId,
                notionWorkspaceName = "Listener Workspace",
                notionBotId = "bot-listener-1",
                occurredAt = OffsetDateTime.now(),
            ),
        )

        val workspace = workspaceRepository.findByNotionWorkspaceId(notionWorkspaceId)
        assertNotNull(workspace)
        assertEquals(1L, workspace.createdBy)
        assertEquals("Listener Workspace", workspace.notionWorkspaceName)
        assertEquals("bot-listener-1", workspace.notionBotId)
    }

    @Test
    fun `같은 notion_workspace_id 두 번 publish — 멱등, 기존 row 유지`() {
        val notionWorkspaceId = "nw-dup-${System.nanoTime()}"
        publisher.publishEvent(
            NotionWorkspaceAuthorizedEvent(1L, notionWorkspaceId, "First", "bot-1", OffsetDateTime.now()),
        )
        val first = workspaceRepository.findByNotionWorkspaceId(notionWorkspaceId)
        assertNotNull(first)
        val firstId = first.id

        publisher.publishEvent(
            NotionWorkspaceAuthorizedEvent(2L, notionWorkspaceId, "Second", "bot-2", OffsetDateTime.now()),
        )

        val second = workspaceRepository.findByNotionWorkspaceId(notionWorkspaceId)
        assertNotNull(second)
        assertEquals(firstId, second.id)
        assertEquals("First", second.notionWorkspaceName)
        assertEquals(1L, second.createdBy)
    }
}
