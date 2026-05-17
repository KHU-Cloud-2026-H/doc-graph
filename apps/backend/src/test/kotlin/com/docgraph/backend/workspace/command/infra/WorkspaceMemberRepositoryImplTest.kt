package com.docgraph.backend.workspace.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    SharedPostgresContainer::class,
    WorkspaceRepositoryImpl::class,
    WorkspaceMemberRepositoryImpl::class,
)
class WorkspaceMemberRepositoryImplTest @Autowired constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val memberRepository: WorkspaceMemberRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `save 후 findByWorkspaceIdAndUserId로 조회`() {
        val workspace = workspaceRepository.save(newWorkspace("nw-1"))
        memberRepository.save(newMember(workspace.id, userId = 100L))
        em.flush()

        val found = memberRepository.findByWorkspaceIdAndUserId(workspace.id, 100L)

        assertNotNull(found)
        assertEquals(workspace.id, found.workspaceId)
        assertEquals(100L, found.userId)
    }

    @Test
    fun `(workspace_id, user_id) UNIQUE 제약 — 중복 시 violation`() {
        val workspace = workspaceRepository.save(newWorkspace("nw-2"))
        memberRepository.save(newMember(workspace.id, 200L))
        em.flush()

        assertThrows<DataIntegrityViolationException> {
            memberRepository.save(newMember(workspace.id, 200L))
            em.flush()
        }
    }

    @Test
    fun `findByWorkspaceId — 워크스페이스의 모든 멤버 반환`() {
        val workspace = workspaceRepository.save(newWorkspace("nw-3"))
        memberRepository.save(newMember(workspace.id, 301L))
        memberRepository.save(newMember(workspace.id, 302L))
        em.flush()

        val members = memberRepository.findByWorkspaceId(workspace.id)

        assertEquals(2, members.size)
        assertTrue(members.map { it.userId }.containsAll(listOf(301L, 302L)))
    }

    @Test
    fun `workspace 삭제 시 멤버 row CASCADE`() {
        val workspace = workspaceRepository.save(newWorkspace("nw-4"))
        memberRepository.save(newMember(workspace.id, 401L))
        em.flush()

        workspaceRepository.delete(workspace)
        em.flush()
        em.clear()

        assertNull(memberRepository.findByWorkspaceIdAndUserId(workspace.id, 401L))
    }

    @Test
    fun `findByWorkspaceIdAndUserId — 없으면 null`() {
        val workspace = workspaceRepository.save(newWorkspace("nw-5"))
        em.flush()

        assertNull(memberRepository.findByWorkspaceIdAndUserId(workspace.id, 999L))
    }

    private fun newWorkspace(notionWorkspaceId: String): Workspace {
        return Workspace(
            notionWorkspaceId = notionWorkspaceId,
            notionWorkspaceName = "Workspace",
            notionBotId = "bot-1",
            createdBy = 1L,
            createdAt = OffsetDateTime.now(),
        )
    }

    private fun newMember(workspaceId: Long, userId: Long): WorkspaceMember {
        return WorkspaceMember(
            workspaceId = workspaceId,
            userId = userId,
            joinedAt = OffsetDateTime.now(),
        )
    }
}
