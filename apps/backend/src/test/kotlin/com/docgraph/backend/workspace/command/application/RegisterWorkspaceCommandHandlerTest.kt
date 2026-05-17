package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@Tag("unit")
class RegisterWorkspaceCommandHandlerTest {

    private val workspaceRepository = mockk<WorkspaceRepository>()
    private val handler = RegisterWorkspaceCommandHandler(workspaceRepository)

    @Test
    fun `새 notion_workspace_id — save 후 새 id 반환`() {
        every { workspaceRepository.findByNotionWorkspaceId("nw-new") } returns null
        val captured = slot<Workspace>()
        every { workspaceRepository.save(capture(captured)) } answers {
            captured.captured.also { it.id = 42L }
        }

        val id = handler.handle(RegisterWorkspaceCommand(1L, "nw-new", "Workspace", "bot-1"))

        assertEquals(42L, id)
        assertEquals("nw-new", captured.captured.notionWorkspaceId)
        assertEquals("Workspace", captured.captured.notionWorkspaceName)
        assertEquals("bot-1", captured.captured.notionBotId)
        assertEquals(1L, captured.captured.createdBy)
        verify(exactly = 1) { workspaceRepository.save(any()) }
    }

    @Test
    fun `같은 notion_workspace_id 존재 — 멱등, save 호출 없이 기존 id 반환`() {
        val existing = Workspace(
            id = 99L,
            notionWorkspaceId = "nw-dup",
            notionWorkspaceName = "Existing",
            notionBotId = "bot-old",
            createdBy = 1L,
            createdAt = OffsetDateTime.now(),
        )
        every { workspaceRepository.findByNotionWorkspaceId("nw-dup") } returns existing

        val id = handler.handle(RegisterWorkspaceCommand(2L, "nw-dup", "Workspace", "bot-new"))

        assertEquals(99L, id)
        verify(exactly = 0) { workspaceRepository.save(any()) }
    }
}
