package com.docgraph.backend.validation.query.infra

import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.QConflict
import com.docgraph.backend.validation.command.domain.QConflictFinding
import com.docgraph.backend.validation.command.domain.QValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ValidationQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)

    fun findConflictsByEdgeIds(edgeIds: List<Long>, pageable: Pageable): Page<Conflict> {
        val c = QConflict.conflict

        val count = queryFactory
            .select(c.count())
            .from(c)
            .where(
                c.edgeId.`in`(edgeIds),
                c.resolvedAt.isNull,
            )
            .fetchOne() ?: 0L

        val content = queryFactory
            .selectFrom(c)
            .where(
                c.edgeId.`in`(edgeIds),
                c.resolvedAt.isNull,
            )
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, count)
    }

    fun findFindingsByConflictIds(conflictIds: List<Long>): Map<Long, List<ConflictFinding>> {
        if (conflictIds.isEmpty()) return emptyMap()

        val f = QConflictFinding.conflictFinding

        return queryFactory
            .selectFrom(f)
            .where(f.conflictId.`in`(conflictIds))
            .fetch()
            .groupBy { it.conflictId }
    }

    fun findValidationTasksByEdgeIds(edgeIds: List<Long>, pageable: Pageable): Page<ValidationTask> {
        val t = QValidationTask.validationTask

        val count = queryFactory
            .select(t.count())
            .from(t)
            .where(t.edgeId.`in`(edgeIds))
            .fetchOne() ?: 0L

        val content = queryFactory
            .selectFrom(t)
            .where(t.edgeId.`in`(edgeIds))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, count)
    }
}