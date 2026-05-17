package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.auth.query.application.FindUserIdByEmailQuery
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceInviteeNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberDuplicateException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspacePermissionDeniedException
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceSelfInviteException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@Tag("unit")
class InviteWorkspaceMemberCommandHandlerTest {

    private val workspaceRepository = mockk<WorkspaceRepository>()
    private val memberRepository = mockk<WorkspaceMemberRepository>()
    private val findUserIdByEmail = mockk<FindUserIdByEmailQuery>()
    private val handler = InviteWorkspaceMemberCommandHandler(
        workspaceRepository,
        memberRepository,
        findUserIdByEmail,
    )

    private fun workspace(id: Long = 10L, createdBy: Long = 1L): Workspace = Workspace(
        id = id,
        notionWorkspaceId = "nw-$id",
        notionWorkspaceName = "Workspace",
        notionBotId = "bot-$id",
        createdBy = createdBy,
        createdAt = OffsetDateTime.now(),
    )

    @Test
    fun `정상 흐름 — invitee save 후 id 반환`() {
        every { workspaceRepository.findById(10L) } returns workspace()
        every { findUserIdByEmail.find("invitee@example.com") } returns 200L
        every { memberRepository.findByWorkspaceIdAndUserId(10L, 200L) } returns null
        val captured = slot<WorkspaceMember>()
        every { memberRepository.save(capture(captured)) } answers {
            captured.captured.also { it.id = 555L }
        }

        val id = handler.handle(InviteWorkspaceMemberCommand(10L, 1L, "invitee@example.com"))

        assertEquals(555L, id)
        assertEquals(10L, captured.captured.workspaceId)
        assertEquals(200L, captured.captured.userId)
    }

    @Test
    fun `workspace 없음 — WorkspaceNotFoundException`() {
        every { workspaceRepository.findById(10L) } returns null

        assertThrows(WorkspaceNotFoundException::class.java) {
            handler.handle(InviteWorkspaceMemberCommand(10L, 1L, "invitee@example.com"))
        }
        verify(exactly = 0) { memberRepository.save(any()) }
    }

    @Test
    fun `생성자가 아닌 요청자 — WorkspacePermissionDeniedException`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)

        assertThrows(WorkspacePermissionDeniedException::class.java) {
            handler.handle(InviteWorkspaceMemberCommand(10L, 999L, "invitee@example.com"))
        }
    }

    @Test
    fun `invitee 이메일 미가입 — WorkspaceInviteeNotFoundException`() {
        every { workspaceRepository.findById(10L) } returns workspace()
        every { findUserIdByEmail.find("missing@example.com") } returns null

        assertThrows(WorkspaceInviteeNotFoundException::class.java) {
            handler.handle(InviteWorkspaceMemberCommand(10L, 1L, "missing@example.com"))
        }
    }

    @Test
    fun `생성자 본인 초대 — WorkspaceSelfInviteException`() {
        every { workspaceRepository.findById(10L) } returns workspace(createdBy = 1L)
        every { findUserIdByEmail.find("creator@example.com") } returns 1L

        assertThrows(WorkspaceSelfInviteException::class.java) {
            handler.handle(InviteWorkspaceMemberCommand(10L, 1L, "creator@example.com"))
        }
    }

    @Test
    fun `중복 초대 — WorkspaceMemberDuplicateException`() {
        every { workspaceRepository.findById(10L) } returns workspace()
        every { findUserIdByEmail.find("dup@example.com") } returns 200L
        every { memberRepository.findByWorkspaceIdAndUserId(10L, 200L) } returns WorkspaceMember(
            id = 1L, workspaceId = 10L, userId = 200L, joinedAt = OffsetDateTime.now(),
        )

        assertThrows(WorkspaceMemberDuplicateException::class.java) {
            handler.handle(InviteWorkspaceMemberCommand(10L, 1L, "dup@example.com"))
        }
    }
}
