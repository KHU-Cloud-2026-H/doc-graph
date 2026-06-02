package com.docgraph.backend.validation.query.infra

import com.docgraph.backend.validation.command.domain.QAiUsageRecord
import com.docgraph.backend.validation.query.application.AiUsageByModel
import com.docgraph.backend.validation.query.application.AiUsageRecordResponse
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AiUsageQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)

    fun aggregateByModel(projectId: Long): List<AiUsageByModel> {
        val r = QAiUsageRecord.aiUsageRecord
        return queryFactory
            .select(
                Projections.constructor(
                    AiUsageByModel::class.java,
                    r.model,
                    r.count(),
                    r.promptTokens.sumLong(),
                    r.completionTokens.sumLong(),
                    r.totalTokens.sumLong(),
                ),
            )
            .from(r)
            .where(r.projectId.eq(projectId))
            .groupBy(r.model)
            .fetch()
    }

    fun findRecordsByProject(projectId: Long, pageable: Pageable): Page<AiUsageRecordResponse> {
        val r = QAiUsageRecord.aiUsageRecord

        val count = queryFactory
            .select(r.count())
            .from(r)
            .where(r.projectId.eq(projectId))
            .fetchOne() ?: 0L

        val content = queryFactory
            .select(
                Projections.constructor(
                    AiUsageRecordResponse::class.java,
                    r.validationTaskId,
                    r.model,
                    r.promptTokens,
                    r.completionTokens,
                    r.totalTokens,
                    r.createdAt,
                ),
            )
            .from(r)
            .where(r.projectId.eq(projectId))
            .orderBy(r.createdAt.desc(), r.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, count)
    }
}
