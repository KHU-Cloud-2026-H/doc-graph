package com.docgraph.backend.validation.query.infra

import com.docgraph.backend.validation.command.domain.QConflict
import com.docgraph.backend.validation.command.domain.QConflictFinding
import com.docgraph.backend.validation.command.domain.QValidationTask
import com.docgraph.backend.validation.query.application.ConflictFindingDetailRow
import com.docgraph.backend.validation.query.application.ConflictFindingRow
import com.docgraph.backend.validation.query.application.ConflictRow
import com.docgraph.backend.validation.query.application.InboxConflictRow
import com.docgraph.backend.validation.query.application.MyConflictStatusFilter
import com.docgraph.backend.validation.query.application.ValidationTaskRow
import com.querydsl.core.types.Projections
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

    fun findConflictsByEdgeIds(edgeIds: List<Long>, pageable: Pageable): Page<ConflictRow> {
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
            .select(
                Projections.constructor(
                    ConflictRow::class.java,
                    c.id,
                    c.edgeId,
                    c.ignoredAt,
                    c.ignoreReason,
                ),
            )
            .from(c)
            .where(
                c.edgeId.`in`(edgeIds),
                c.resolvedAt.isNull,
            )
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, count)
    }

    fun findInboxConflictsByEdgeIds(
        edgeIds: List<Long>,
        filter: MyConflictStatusFilter,
        pageable: Pageable,
    ): Page<InboxConflictRow> {
        if (edgeIds.isEmpty()) return PageImpl(emptyList(), pageable, 0L)
        val c = QConflict.conflict
        val filterPredicate = when (filter) {
            MyConflictStatusFilter.ACTIVE -> c.ignoredAt.isNull
            MyConflictStatusFilter.IGNORED -> c.ignoredAt.isNotNull
            MyConflictStatusFilter.ALL -> null
        }

        val count = queryFactory
            .select(c.count())
            .from(c)
            .where(c.edgeId.`in`(edgeIds), c.resolvedAt.isNull, filterPredicate)
            .fetchOne() ?: 0L

        val content = queryFactory
            .select(
                Projections.constructor(
                    InboxConflictRow::class.java,
                    c.id,
                    c.edgeId,
                    c.firstDetectedAt,
                    c.ignoredAt,
                ),
            )
            .from(c)
            .where(c.edgeId.`in`(edgeIds), c.resolvedAt.isNull, filterPredicate)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, count)
    }

    fun findFindingsByConflictIds(conflictIds: List<Long>): List<ConflictFindingRow> {
        if (conflictIds.isEmpty()) return emptyList()

        val f = QConflictFinding.conflictFinding

        return queryFactory
            .select(
                Projections.constructor(
                    ConflictFindingRow::class.java,
                    f.id,
                    f.conflictId,
                    f.sourceBlockIds,
                    f.targetBlockId,
                    f.rationale,
                    f.newText,
                    f.title,
                    f.detectedAt,
                ),
            )
            .from(f)
            .where(f.conflictId.`in`(conflictIds))
            .fetch()
    }

    fun findLatestFindingTitleByConflictId(conflictId: Long): String? {
        val f = QConflictFinding.conflictFinding
        return queryFactory
            .select(f.title)
            .from(f)
            .where(f.conflictId.eq(conflictId))
            .orderBy(f.detectedAt.desc(), f.id.desc())
            .fetchFirst()
    }

    fun findFindingDetailById(findingId: Long): ConflictFindingDetailRow? {
        val f = QConflictFinding.conflictFinding
        val c = QConflict.conflict

        return queryFactory
            .select(
                Projections.constructor(
                    ConflictFindingDetailRow::class.java,
                    f.id,
                    c.edgeId,
                    f.targetBlockId,
                    f.newText,
                ),
            )
            .from(f)
            .join(c).on(c.id.eq(f.conflictId))
            .where(f.id.eq(findingId))
            .fetchOne()
    }

    fun findActiveUnignoredEdgeIds(edgeIds: Collection<Long>): List<Long> {
        if (edgeIds.isEmpty()) return emptyList()
        val c = QConflict.conflict
        return queryFactory
            .select(c.edgeId)
            .from(c)
            .where(
                c.edgeId.`in`(edgeIds),
                c.resolvedAt.isNull,
                c.ignoredAt.isNull,
            )
            .distinct()
            .fetch()
    }

    fun findValidationTasksByEdgeIds(edgeIds: List<Long>, pageable: Pageable): Page<ValidationTaskRow> {
        val t = QValidationTask.validationTask

        val count = queryFactory
            .select(t.count())
            .from(t)
            .where(t.edgeId.`in`(edgeIds))
            .fetchOne() ?: 0L

        val content = queryFactory
            .select(
                Projections.constructor(
                    ValidationTaskRow::class.java,
                    t.id,
                    t.edgeId,
                    t.status,
                    t.createdAt,
                ),
            )
            .from(t)
            .where(t.edgeId.`in`(edgeIds))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, count)
    }
}
