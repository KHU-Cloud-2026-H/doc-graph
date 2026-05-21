package com.docgraph.backend.workspace.query.infra

import com.docgraph.backend.workspace.command.domain.QWorkspace
import com.docgraph.backend.workspace.command.domain.QWorkspaceMember
import com.docgraph.backend.workspace.query.application.WorkspaceMembershipRow
import com.docgraph.backend.workspace.query.application.WorkspaceSummary
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class WorkspaceQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)

    fun findAccessibleWorkspaceSummaries(userId: Long): List<WorkspaceSummary> {
        val w = QWorkspace.workspace
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(
                Projections.constructor(
                    WorkspaceSummary::class.java,
                    w.id,
                    w.notionWorkspaceName,
                ),
            )
            .from(w)
            .leftJoin(wm).on(wm.workspaceId.eq(w.id))
            .where(w.createdBy.eq(userId).or(wm.userId.eq(userId)))
            .distinct()
            .fetch()
    }

    fun findWorkspaceSummaryAccessibleBy(workspaceId: Long, userId: Long): WorkspaceSummary? {
        val w = QWorkspace.workspace
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(
                Projections.constructor(
                    WorkspaceSummary::class.java,
                    w.id,
                    w.notionWorkspaceName,
                ),
            )
            .from(w)
            .leftJoin(wm).on(wm.workspaceId.eq(w.id))
            .where(
                w.id.eq(workspaceId),
                w.createdBy.eq(userId).or(wm.userId.eq(userId)),
            )
            .distinct()
            .fetchFirst()
    }

    fun findMembersByWorkspaceId(workspaceId: Long): List<WorkspaceMembershipRow> {
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(
                Projections.constructor(
                    WorkspaceMembershipRow::class.java,
                    wm.userId,
                    wm.joinedAt,
                ),
            )
            .from(wm)
            .where(wm.workspaceId.eq(workspaceId))
            .fetch()
    }
}