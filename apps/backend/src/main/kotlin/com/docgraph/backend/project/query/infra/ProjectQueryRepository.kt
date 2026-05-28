package com.docgraph.backend.project.query.infra

import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.QCategory
import com.docgraph.backend.project.command.domain.QProject
import com.docgraph.backend.project.command.domain.QProjectMember
import com.docgraph.backend.project.command.domain.QTypeAssigneeDefault
import com.docgraph.backend.project.query.application.AssignedDocumentType
import com.docgraph.backend.project.query.application.CategoryProjection
import com.docgraph.backend.project.query.application.CategoryResponse
import com.docgraph.backend.project.query.application.ProjectMembershipRow
import com.docgraph.backend.project.query.application.ProjectRef
import com.docgraph.backend.project.query.application.ProjectRow
import com.docgraph.backend.project.query.application.TypeAssigneeResponse
import com.docgraph.backend.workspace.command.domain.QWorkspaceMember
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class ProjectQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)

    fun findAccessibleProjectRefsByWorkspace(workspaceId: Long, userId: Long): List<ProjectRef> {
        val p = QProject.project
        val pm = QProjectMember.projectMember
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(Projections.constructor(ProjectRef::class.java, p.id, p.workspaceId, p.name))
            .from(p)
            .join(pm).on(pm.projectId.eq(p.id))
            .join(wm).on(wm.id.eq(pm.workspaceMemberId))
            .where(p.workspaceId.eq(workspaceId), wm.userId.eq(userId))
            .distinct()
            .fetch()
    }

    fun findProjectIfAccessible(projectId: Long, userId: Long): ProjectRow? {
        val p = QProject.project
        val pm = QProjectMember.projectMember
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(Projections.constructor(ProjectRow::class.java, p.id, p.name, p.notionRootPageId))
            .from(p)
            .join(pm).on(pm.projectId.eq(p.id))
            .join(wm).on(wm.id.eq(pm.workspaceMemberId))
            .where(p.id.eq(projectId), wm.userId.eq(userId))
            .distinct()
            .fetchFirst()
    }

    fun findMembersByProjectId(projectId: Long): List<ProjectMembershipRow> {
        val pm = QProjectMember.projectMember
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(Projections.constructor(ProjectMembershipRow::class.java, pm.id, wm.userId, pm.role))
            .from(pm)
            .join(wm).on(wm.id.eq(pm.workspaceMemberId))
            .where(pm.projectId.eq(projectId))
            .fetch()
    }

    fun findCategoryDetailsByProjectId(projectId: Long): List<CategoryResponse> {
        val c = QCategory.category
        return queryFactory
            .select(Projections.constructor(CategoryResponse::class.java, c.id, c.notionPageId, c.documentType))
            .from(c)
            .where(c.projectId.eq(projectId))
            .fetch()
    }

    fun findTypeAssigneesByProjectId(projectId: Long): List<TypeAssigneeResponse> {
        val ta = QTypeAssigneeDefault.typeAssigneeDefault
        return queryFactory
            .select(
                Projections.constructor(
                    TypeAssigneeResponse::class.java,
                    ta.documentType,
                    ta.assigneeWorkspaceMemberId,
                ),
            )
            .from(ta)
            .where(ta.projectId.eq(projectId))
            .fetch()
    }

    fun findAdminProjectIdsByUserId(userId: Long): List<Long> {
        val pm = QProjectMember.projectMember
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(pm.projectId)
            .from(pm)
            .join(wm).on(wm.id.eq(pm.workspaceMemberId))
            .where(wm.userId.eq(userId), pm.role.eq(ProjectMemberRole.ADMIN))
            .distinct()
            .fetch()
    }

    fun findProjectRefsByIds(projectIds: Collection<Long>): List<ProjectRef> {
        if (projectIds.isEmpty()) return emptyList()
        val p = QProject.project
        return queryFactory
            .select(Projections.constructor(ProjectRef::class.java, p.id, p.workspaceId, p.name))
            .from(p)
            .where(p.id.`in`(projectIds))
            .fetch()
    }

    fun findNotionRootPageIdsByProjectIds(projectIds: Collection<Long>): Map<Long, String> {
        if (projectIds.isEmpty()) return emptyMap()
        val p = QProject.project
        return queryFactory
            .select(p.id, p.notionRootPageId)
            .from(p)
            .where(p.id.`in`(projectIds))
            .fetch()
            .associate { it.get(p.id)!! to it.get(p.notionRootPageId)!! }
    }

    fun findProjectMemberCountsByIds(projectIds: Collection<Long>): Map<Long, Int> {
        if (projectIds.isEmpty()) return emptyMap()
        val pm = QProjectMember.projectMember
        return queryFactory
            .select(pm.projectId, pm.count())
            .from(pm)
            .where(pm.projectId.`in`(projectIds))
            .groupBy(pm.projectId)
            .fetch()
            .associate { it.get(pm.projectId)!! to it.get(pm.count())!!.toInt() }
    }

    fun findAssignedDocumentTypesByUserId(userId: Long): List<AssignedDocumentType> {
        val ta = QTypeAssigneeDefault.typeAssigneeDefault
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(
                Projections.constructor(
                    AssignedDocumentType::class.java,
                    ta.projectId,
                    ta.documentType,
                ),
            )
            .from(ta)
            .join(wm).on(wm.id.eq(ta.assigneeWorkspaceMemberId))
            .where(wm.userId.eq(userId))
            .fetch()
    }

    fun findCategoriesByProjectId(projectId: Long): List<CategoryProjection> {
        val c = QCategory.category
        return queryFactory
            .select(Projections.constructor(CategoryProjection::class.java, c.notionPageId, c.documentType))
            .from(c)
            .where(c.projectId.eq(projectId))
            .fetch()
    }

    fun findProjectIdsByWorkspaceIdIn(workspaceIds: Collection<Long>): Map<Long, List<Long>> {
        if (workspaceIds.isEmpty()) return emptyMap()
        val p = QProject.project
        return queryFactory
            .select(p.workspaceId, p.id)
            .from(p)
            .where(p.workspaceId.`in`(workspaceIds))
            .fetch()
            .groupBy({ it.get(p.workspaceId)!! }, { it.get(p.id)!! })
    }
}
