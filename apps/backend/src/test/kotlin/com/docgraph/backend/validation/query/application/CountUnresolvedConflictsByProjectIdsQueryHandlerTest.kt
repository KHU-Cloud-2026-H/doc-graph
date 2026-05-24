package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.SearchEdgeIdsByProjectIdsQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class CountUnresolvedConflictsByProjectIdsQueryHandlerTest {

    private val searchEdgeIdsByProjects = mockk<SearchEdgeIdsByProjectIdsQuery>()
    private val repository = mockk<ValidationQueryRepository>()

    @Test
    fun `count — projectId별 활성 미무시 conflict 개수 반환`() {
        every { searchEdgeIdsByProjects.search(listOf(1L, 2L)) } returns mapOf(
            1L to listOf(10L, 11L, 12L),
            2L to listOf(20L, 21L),
        )
        every { repository.findActiveUnignoredEdgeIds(setOf(10L, 11L, 12L, 20L, 21L)) } returns
            listOf(10L, 12L, 20L)

        val result = CountUnresolvedConflictsByProjectIdsQueryHandler(searchEdgeIdsByProjects, repository)
            .count(listOf(1L, 2L))

        assertEquals(2L, result[1L])
        assertEquals(1L, result[2L])
    }

    @Test
    fun `count — 빈 입력이면 빈 Map`() {
        val result = CountUnresolvedConflictsByProjectIdsQueryHandler(searchEdgeIdsByProjects, repository)
            .count(emptyList())

        assertEquals(emptyMap<Long, Long>(), result)
    }

    @Test
    fun `count — edge 없는 projectId만 입력되면 빈 Map`() {
        every { searchEdgeIdsByProjects.search(listOf(1L)) } returns emptyMap()

        val result = CountUnresolvedConflictsByProjectIdsQueryHandler(searchEdgeIdsByProjects, repository)
            .count(listOf(1L))

        assertEquals(emptyMap<Long, Long>(), result)
    }

    @Test
    fun `count — edge는 있지만 활성 미무시 conflict가 없으면 0L로 반환`() {
        every { searchEdgeIdsByProjects.search(listOf(1L)) } returns mapOf(1L to listOf(10L, 11L))
        every { repository.findActiveUnignoredEdgeIds(setOf(10L, 11L)) } returns emptyList()

        val result = CountUnresolvedConflictsByProjectIdsQueryHandler(searchEdgeIdsByProjects, repository)
            .count(listOf(1L))

        assertEquals(0L, result[1L])
    }
}
