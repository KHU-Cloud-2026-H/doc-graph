package com.docgraph.backend.project.query.infra

import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.QCategory
import com.docgraph.backend.project.command.domain.QProject
import com.docgraph.backend.project.command.domain.QProjectMember
import com.docgraph.backend.project.command.domain.QTypeAssigneeDefault
import com.docgraph.backend.project.query.application.CategoryProjection
import com.docgraph.backend.project.query.application.CategoryResponse
import com.docgraph.backend.project.query.application.ProjectMembershipRow
import com.docgraph.backend.project.query.application.ProjectRow
import com.docgraph.backend.project.query.application.ProjectSummary
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

    fun findAccessibleProjectSummaries(workspaceId: Long, userId: Long): List<ProjectSummary> {
        val p = QProject.project
        val pm = QProjectMember.projectMember
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(Projections.constructor(ProjectSummary::class.java, p.id, p.name))
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

    fun findCategoriesByProjectId(projectId: Long): List<CategoryProjection> {
        val c = QCategory.category
        return queryFactory
            .select(Projections.constructor(CategoryProjection::class.java, c.notionPageId, c.documentType))
            .from(c)
            .where(c.projectId.eq(projectId))
            .fetch()
    }
}
