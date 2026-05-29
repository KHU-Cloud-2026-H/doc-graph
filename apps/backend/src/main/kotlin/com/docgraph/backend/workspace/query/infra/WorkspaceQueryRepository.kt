package com.docgraph.backend.workspace.query.infra

import com.docgraph.backend.workspace.command.domain.QWorkspace
import com.docgraph.backend.workspace.command.domain.QWorkspaceMember
import com.docgraph.backend.workspace.query.application.WorkspaceMembershipRow
import com.docgraph.backend.workspace.query.application.WorkspaceRef
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

    fun findAccessibleWorkspaceRefs(userId: Long): List<WorkspaceRef> {
        val w = QWorkspace.workspace
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(
                Projections.constructor(
                    WorkspaceRef::class.java,
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

    fun findWorkspaceRefAccessibleBy(workspaceId: Long, userId: Long): WorkspaceRef? {
        val w = QWorkspace.workspace
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(
                Projections.constructor(
                    WorkspaceRef::class.java,
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

    fun findMemberIdsByUserId(userId: Long): List<Long> {
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(wm.id)
            .from(wm)
            .where(wm.userId.eq(userId))
            .fetch()
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

    fun findMemberCountsByWorkspaceIds(workspaceIds: Collection<Long>): Map<Long, Int> {
        if (workspaceIds.isEmpty()) return emptyMap()
        val wm = QWorkspaceMember.workspaceMember
        return queryFactory
            .select(wm.workspaceId, wm.count())
            .from(wm)
            .where(wm.workspaceId.`in`(workspaceIds))
            .groupBy(wm.workspaceId)
            .fetch()
            .associate { it.get(wm.workspaceId)!! to it.get(wm.count())!!.toInt() }
    }
}
