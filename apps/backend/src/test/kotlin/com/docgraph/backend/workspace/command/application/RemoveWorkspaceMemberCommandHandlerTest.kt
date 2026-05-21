package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceCreatorRemovalException
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspacePermissionDeniedException
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class RemoveWorkspaceMemberCommandHandlerTest {

    private val workspaceRepository = mockk<WorkspaceRepository>()
    private val memberRepository = mockk<WorkspaceMemberRepository>(relaxUnitFun = true)
    private val handler = RemoveWorkspaceMemberCommandHandler(workspaceRepository, memberRepository)

    private fun workspace(id: Long = 10L, createdBy: Long = 1L): Workspace = Workspace(
        id = id,
        notionWorkspaceId = "nw-$id",
        notionWorkspaceName = "Workspace",
        notionBotId = "bot-$id",
        createdBy = createdBy,
        createdAt = OffsetDateTime.now(),
    )

    private fun member(memberId: Long, workspaceId: Long, userId: Long): WorkspaceMember = WorkspaceMember(
        id = memberId,
        workspaceId = workspaceId,
        userId = userId,
        joinedAt = OffsetDateTime.now(),
    )

    @Test
    fun `정상 흐름 — delete 호출`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)
        val target = member(memberId = 200L, workspaceId = 10L, userId = 200L)
        every { memberRepository.findById(200L) } returns target

        handler.handle(RemoveWorkspaceMemberCommand(10L, 1L, 200L))

        verify { memberRepository.delete(target) }
    }

    @Test
    fun `workspace 없음 — WorkspaceNotFoundException`() {
        every { workspaceRepository.findById(10L) } returns null

        assertThrows(WorkspaceNotFoundException::class.java) {
            handler.handle(RemoveWorkspaceMemberCommand(10L, 1L, 200L))
        }
    }

    @Test
    fun `생성자가 아닌 요청자 — WorkspacePermissionDeniedException`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)

        assertThrows(WorkspacePermissionDeniedException::class.java) {
            handler.handle(RemoveWorkspaceMemberCommand(10L, 999L, 200L))
        }
    }

    @Test
    fun `생성자 본인 제거 시도 — WorkspaceCreatorRemovalException`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)
        every { memberRepository.findById(1L) } returns
            member(memberId = 1L, workspaceId = 10L, userId = 1L)

        assertThrows(WorkspaceCreatorRemovalException::class.java) {
            handler.handle(RemoveWorkspaceMemberCommand(10L, 1L, 1L))
        }
    }

    @Test
    fun `존재하지 않는 멤버 제거 — WorkspaceMemberNotFoundException`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)
        every { memberRepository.findById(200L) } returns null

        assertThrows(WorkspaceMemberNotFoundException::class.java) {
            handler.handle(RemoveWorkspaceMemberCommand(10L, 1L, 200L))
        }
    }

    @Test
    fun `다른 워크스페이스 소속 member id — WorkspaceMemberNotFoundException`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)
        every { memberRepository.findById(200L) } returns
            member(memberId = 200L, workspaceId = 99L, userId = 200L)

        assertThrows(WorkspaceMemberNotFoundException::class.java) {
            handler.handle(RemoveWorkspaceMemberCommand(10L, 1L, 200L))
        }
    }
}
