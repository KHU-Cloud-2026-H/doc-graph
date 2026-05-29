package com.docgraph.backend.project.query.infra

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import com.docgraph.backend.project.command.infra.ProjectJpaRepository
import com.docgraph.backend.project.command.infra.TypeAssigneeDefaultJpaRepository
import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.infra.ProjectMemberJpaRepository
import com.docgraph.backend.project.query.application.AssignedDocumentType
import com.docgraph.backend.project.query.application.ProjectRef
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.infra.WorkspaceJpaRepository
import com.docgraph.backend.workspace.command.infra.WorkspaceMemberJpaRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, ProjectQueryRepository::class)
class ProjectQueryRepositoryTest @Autowired constructor(
    private val queryRepository: ProjectQueryRepository,
    private val workspaceJpa: WorkspaceJpaRepository,
    private val workspaceMemberJpa: WorkspaceMemberJpaRepository,
    private val projectJpa: ProjectJpaRepository,
    private val projectMemberJpa: ProjectMemberJpaRepository,
    private val typeAssigneeJpa: TypeAssigneeDefaultJpaRepository,
) {

    @Test
    fun `findAssignedDocumentTypesByUserId — user 워크스페이스 멤버가 default assignee로 매핑된 type 반환`() {
        val workspace = workspaceJpa.save(newWorkspace("ws-1"))
        val member = workspaceMemberJpa.save(WorkspaceMember(workspaceId = workspace.id, userId = 100L))
        val project = projectJpa.save(newProject(workspace.id, "P"))
        typeAssigneeJpa.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.REQUIREMENTS,
                assigneeWorkspaceMemberId = member.id,
            ),
        )

        val result = queryRepository.findAssignedDocumentTypesByUserId(100L)

        assertEquals(1, result.size)
        assertEquals(project.id, result[0].projectId)
        assertEquals(DocumentType.REQUIREMENTS, result[0].documentType)
    }

    @Test
    fun `findAssignedDocumentTypesByUserId — 다른 user의 매핑은 제외`() {
        val workspace = workspaceJpa.save(newWorkspace("ws-2"))
        workspaceMemberJpa.save(WorkspaceMember(workspaceId = workspace.id, userId = 100L))
        val member200 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = workspace.id, userId = 200L))
        val project = projectJpa.save(newProject(workspace.id, "P"))
        typeAssigneeJpa.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.REQUIREMENTS,
                assigneeWorkspaceMemberId = member200.id,
            ),
        )

        val result = queryRepository.findAssignedDocumentTypesByUserId(100L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAssignedDocumentTypesByUserId — 매핑 없으면 빈 리스트`() {
        val result = queryRepository.findAssignedDocumentTypesByUserId(99999L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAssignedDocumentTypesByUserId — 한 user가 여러 workspace 가입 시 모든 workspace 매핑 반환`() {
        val ws1 = workspaceJpa.save(newWorkspace("ws-a"))
        val ws2 = workspaceJpa.save(newWorkspace("ws-b"))
        val m1 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws1.id, userId = 100L))
        val m2 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws2.id, userId = 100L))
        val p1 = projectJpa.save(newProject(ws1.id, "P1"))
        val p2 = projectJpa.save(newProject(ws2.id, "P2"))
        typeAssigneeJpa.save(
            TypeAssigneeDefault(
                projectId = p1.id,
                documentType = DocumentType.PLANNING,
                assigneeWorkspaceMemberId = m1.id,
            ),
        )
        typeAssigneeJpa.save(
            TypeAssigneeDefault(
                projectId = p2.id,
                documentType = DocumentType.REQUIREMENTS,
                assigneeWorkspaceMemberId = m2.id,
            ),
        )

        val result = queryRepository.findAssignedDocumentTypesByUserId(100L).toSet()

        assertEquals(
            setOf(
                AssignedDocumentType(p1.id, DocumentType.PLANNING),
                AssignedDocumentType(p2.id, DocumentType.REQUIREMENTS),
            ),
            result,
        )
    }

    @Test
    fun `findAssignedDocumentTypesByUserId — assigneeWorkspaceMemberId가 null인 매핑은 제외`() {
        val workspace = workspaceJpa.save(newWorkspace("ws-3"))
        workspaceMemberJpa.save(WorkspaceMember(workspaceId = workspace.id, userId = 100L))
        val project = projectJpa.save(newProject(workspace.id, "P"))
        typeAssigneeJpa.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.REQUIREMENTS,
                assigneeWorkspaceMemberId = null,
            ),
        )

        val result = queryRepository.findAssignedDocumentTypesByUserId(100L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findProjectRefsByIds — 매칭되는 project를 id와 name으로 반환`() {
        val ws = workspaceJpa.save(newWorkspace("ws-sum-1"))
        val p1 = projectJpa.save(newProject(ws.id, "Q2 Planning"))
        val p2 = projectJpa.save(newProject(ws.id, "Spec Drafts"))

        val result = queryRepository.findProjectRefsByIds(listOf(p1.id, p2.id)).toSet()

        assertEquals(
            setOf(
                ProjectRef(p1.id, ws.id, "Q2 Planning"),
                ProjectRef(p2.id, ws.id, "Spec Drafts"),
            ),
            result,
        )
    }

    @Test
    fun `findProjectRefsByIds — 일부 id만 매칭되면 매칭된 것만 반환`() {
        val ws = workspaceJpa.save(newWorkspace("ws-sum-2"))
        val p = projectJpa.save(newProject(ws.id, "Only"))

        val result = queryRepository.findProjectRefsByIds(listOf(p.id, 99999L))

        assertEquals(listOf(ProjectRef(p.id, ws.id, "Only")), result)
    }

    @Test
    fun `findProjectRefsByIds — 입력 빈 리스트면 빈 리스트`() {
        val result = queryRepository.findProjectRefsByIds(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findProjectRefsByIds — 매칭 없으면 빈 리스트`() {
        val result = queryRepository.findProjectRefsByIds(listOf(99999L, 88888L))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findProjectMemberCountsByIds — projectId별 멤버 수 반환`() {
        val ws = workspaceJpa.save(newWorkspace("ws-mc-1"))
        val wm1 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws.id, userId = 1L))
        val wm2 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws.id, userId = 2L))
        val wm3 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws.id, userId = 3L))
        val p1 = projectJpa.save(newProject(ws.id, "P1"))
        val p2 = projectJpa.save(newProject(ws.id, "P2"))
        projectMemberJpa.save(ProjectMember(projectId = p1.id, workspaceMemberId = wm1.id, role = ProjectMemberRole.ADMIN))
        projectMemberJpa.save(ProjectMember(projectId = p1.id, workspaceMemberId = wm2.id, role = ProjectMemberRole.MEMBER))
        projectMemberJpa.save(ProjectMember(projectId = p2.id, workspaceMemberId = wm3.id, role = ProjectMemberRole.ADMIN))

        val result = queryRepository.findProjectMemberCountsByIds(listOf(p1.id, p2.id))

        assertEquals(2, result[p1.id])
        assertEquals(1, result[p2.id])
    }

    @Test
    fun `findProjectMemberCountsByIds — 빈 입력이면 빈 Map`() {
        val result = queryRepository.findProjectMemberCountsByIds(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findProjectMemberCountsByIds — 멤버 없는 project는 Map에 미포함`() {
        val ws = workspaceJpa.save(newWorkspace("ws-mc-2"))
        val p1 = projectJpa.save(newProject(ws.id, "EmptyP"))

        val result = queryRepository.findProjectMemberCountsByIds(listOf(p1.id))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findProjectIdsByWorkspaceIdIn — workspace별 project ids 그룹화 반환`() {
        val ws1 = workspaceJpa.save(newWorkspace("ws-grp-1"))
        val ws2 = workspaceJpa.save(newWorkspace("ws-grp-2"))
        val p1a = projectJpa.save(newProject(ws1.id, "A"))
        val p1b = projectJpa.save(newProject(ws1.id, "B"))
        val p2a = projectJpa.save(newProject(ws2.id, "C"))

        val result = queryRepository.findProjectIdsByWorkspaceIdIn(listOf(ws1.id, ws2.id))

        assertEquals(setOf(p1a.id, p1b.id), result[ws1.id]?.toSet())
        assertEquals(listOf(p2a.id), result[ws2.id])
    }

    @Test
    fun `findProjectIdsByWorkspaceIdIn — 빈 입력이면 빈 Map`() {
        val result = queryRepository.findProjectIdsByWorkspaceIdIn(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findProjectIdsByWorkspaceIdIn — project 없는 workspace는 Map에 미포함`() {
        val ws1 = workspaceJpa.save(newWorkspace("ws-empty-1"))
        val ws2 = workspaceJpa.save(newWorkspace("ws-empty-2"))
        projectJpa.save(newProject(ws1.id, "Only"))

        val result = queryRepository.findProjectIdsByWorkspaceIdIn(listOf(ws1.id, ws2.id))

        assertEquals(1, result.size)
        assertTrue(result.containsKey(ws1.id))
        assertTrue(!result.containsKey(ws2.id))
    }

    @Test
    fun `findProjectIdsByWorkspaceIdIn — 입력에 포함되지 않은 workspace는 제외`() {
        val ws1 = workspaceJpa.save(newWorkspace("ws-excl-1"))
        val ws2 = workspaceJpa.save(newWorkspace("ws-excl-2"))
        projectJpa.save(newProject(ws1.id, "A"))
        projectJpa.save(newProject(ws2.id, "B"))

        val result = queryRepository.findProjectIdsByWorkspaceIdIn(listOf(ws1.id))

        assertEquals(setOf(ws1.id), result.keys)
    }

    private fun newWorkspace(notionId: String) = Workspace(
        notionWorkspaceId = notionId,
        notionWorkspaceName = notionId,
        notionBotId = "bot-$notionId",
        createdBy = 1L,
    )

    private fun newProject(workspaceId: Long, name: String) = Project(
        workspaceId = workspaceId,
        name = name,
        notionRootPageId = "root-$name",
        createdBy = 1L,
    )
}
