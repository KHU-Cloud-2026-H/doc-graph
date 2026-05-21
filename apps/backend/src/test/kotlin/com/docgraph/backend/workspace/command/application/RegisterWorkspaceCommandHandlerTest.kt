package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
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
    private val workspaceMemberRepository = mockk<WorkspaceMemberRepository>()
    private val handler = RegisterWorkspaceCommandHandler(workspaceRepository, workspaceMemberRepository)

    @Test
    fun `새 notion_workspace_id — workspace + 생성자 workspace_member 동시 save, 새 id 반환`() {
        every { workspaceRepository.findByNotionWorkspaceId("nw-new") } returns null
        val capturedWorkspace = slot<Workspace>()
        every { workspaceRepository.save(capture(capturedWorkspace)) } answers {
            capturedWorkspace.captured.also { it.id = 42L }
        }
        val capturedMember = slot<WorkspaceMember>()
        every { workspaceMemberRepository.save(capture(capturedMember)) } answers {
            capturedMember.captured.also { it.id = 100L }
        }

        val id = handler.handle(RegisterWorkspaceCommand(1L, "nw-new", "Workspace", "bot-1"))

        assertEquals(42L, id)
        assertEquals("nw-new", capturedWorkspace.captured.notionWorkspaceId)
        assertEquals(1L, capturedWorkspace.captured.createdBy)
        assertEquals(42L, capturedMember.captured.workspaceId)
        assertEquals(1L, capturedMember.captured.userId)
        verify(exactly = 1) { workspaceRepository.save(any()) }
        verify(exactly = 1) { workspaceMemberRepository.save(any()) }
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
        verify(exactly = 0) { workspaceMemberRepository.save(any()) }
    }
}
