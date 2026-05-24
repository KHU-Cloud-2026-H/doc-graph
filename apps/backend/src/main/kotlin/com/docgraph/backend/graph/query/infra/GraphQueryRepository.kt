package com.docgraph.backend.graph.query.infra

import com.docgraph.backend.graph.command.domain.QDependencyEdge
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class GraphQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)

    fun findEdgeIdsByProjectIdIn(projectIds: Collection<Long>): Map<Long, List<Long>> {
        if (projectIds.isEmpty()) return emptyMap()
        val e = QDependencyEdge.dependencyEdge
        return queryFactory
            .select(e.projectId, e.id)
            .from(e)
            .where(e.projectId.`in`(projectIds))
            .fetch()
            .groupBy({ it.get(e.projectId)!! }, { it.get(e.id)!! })
    }
}