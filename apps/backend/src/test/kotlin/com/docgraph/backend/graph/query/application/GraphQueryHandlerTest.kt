package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class GraphQueryHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>()

    @Test
    fun `FindEdgeByIdQueryHandler — edge detail 반환`() {
        every { edgeRepository.findById(100L) } returns edge(id = 100L)

        val result = FindEdgeByIdQueryHandler(edgeRepository).find(100L)

        assertEquals(100L, result!!.id)
        assertEquals(10L, result.sourceDocumentId)
        assertEquals(20L, result.targetDocumentId)
        assertEquals("범위 일치 여부", result.validationCriterion)
    }

    @Test
    fun `FindEdgeByIdQueryHandler — edge 없으면 null`() {
        every { edgeRepository.findById(100L) } returns null

        assertNull(FindEdgeByIdQueryHandler(edgeRepository).find(100L))
    }

    @Test
    fun `SearchEdgeDetailsByProjectQueryHandler — 프로젝트 edge details 반환`() {
        every { edgeRepository.findAllByProjectId(1L) } returns listOf(edge(id = 100L), edge(id = 101L))

        val result = SearchEdgeDetailsByProjectQueryHandler(edgeRepository).search(1L)

        assertEquals(listOf(100L, 101L), result.map { it.id })
    }

    @Test
    fun `SearchEdgeIdsByProjectQueryHandler — 프로젝트 edge ids 반환`() {
        every { edgeRepository.findAllByProjectId(1L) } returns listOf(edge(id = 100L), edge(id = 101L))

        val result = SearchEdgeIdsByProjectQueryHandler(edgeRepository).search(1L)

        assertEquals(listOf(100L, 101L), result)
    }

    @Test
    fun `SearchEdgeIdsByTargetDocumentIdsQueryHandler — target 문서 기준 edge ids 반환`() {
        every { edgeRepository.findAllByTargetDocumentIdIn(listOf(20L, 21L)) } returns
            listOf(edge(id = 100L), edge(id = 101L))

        val result = SearchEdgeIdsByTargetDocumentIdsQueryHandler(edgeRepository).search(listOf(20L, 21L))

        assertEquals(listOf(100L, 101L), result)
    }

    private fun edge(id: Long): DependencyEdge =
        DependencyEdge(
            id = id,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
        )
}
