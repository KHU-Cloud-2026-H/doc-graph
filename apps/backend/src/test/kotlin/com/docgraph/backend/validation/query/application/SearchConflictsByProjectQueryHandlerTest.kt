package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.SearchEdgeDetailsByProjectQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class SearchConflictsByProjectQueryHandlerTest {

    private val searchEdgeDetailsByProject = mockk<SearchEdgeDetailsByProjectQuery>()
    private val validationQueryRepository = mockk<ValidationQueryRepository>()

    private val handler = SearchConflictsByProjectQueryHandler(
        searchEdgeDetailsByProject,
        validationQueryRepository,
    )

    private val now = OffsetDateTime.now()

    @Test
    fun `빈 edge list면 빈 PageResponse 반환`() {
        every { searchEdgeDetailsByProject.search(1L) } returns emptyList()

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(0L, result.totalElements)
        assertTrue(result.content.isEmpty())
    }

    @Test
    fun `edgeDetail의 sourceDocumentId, targetDocumentId가 ConflictResponse에 정확히 매핑`() {
        val edgeDetail = EdgeDetail(id = 10L, projectId = 1L, sourceDocumentId = 100L, targetDocumentId = 200L, validationCriterion = "criterion")
        val conflictRow = ConflictRow(id = 1L, edgeId = 10L, ignoredAt = null, ignoreReason = null)

        every { searchEdgeDetailsByProject.search(1L) } returns listOf(edgeDetail)
        every { validationQueryRepository.findConflictsByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(conflictRow), PageRequest.of(0, 20), 1L)
        every { validationQueryRepository.findFindingsByConflictIds(listOf(1L)) } returns emptyList()

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(1, result.content.size)
        val response = result.content[0]
        assertEquals(10L, response.edgeId)
        assertEquals(100L, response.sourceDocumentId)
        assertEquals(200L, response.targetDocumentId)
    }

    @Test
    fun `findings가 conflictId별로 정확히 grouped되어 매핑`() {
        val edgeDetail = EdgeDetail(id = 10L, projectId = 1L, sourceDocumentId = 100L, targetDocumentId = 200L, validationCriterion = "criterion")
        val conflictRow = ConflictRow(id = 1L, edgeId = 10L, ignoredAt = null, ignoreReason = null)
        val finding1 = ConflictFindingRow(
            id = 11L, conflictId = 1L,
            sourceBlockIds = listOf("a"), targetBlockId = "b",
            rationale = "rationale1", newText = "suggestion1", title = "ttl1", detectedAt = now,
        )
        val finding2 = ConflictFindingRow(
            id = 12L, conflictId = 1L,
            sourceBlockIds = listOf("c"), targetBlockId = "d",
            rationale = "rationale2", newText = "suggestion2", title = "ttl2", detectedAt = now,
        )

        every { searchEdgeDetailsByProject.search(1L) } returns listOf(edgeDetail)
        every { validationQueryRepository.findConflictsByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(conflictRow), PageRequest.of(0, 20), 1L)
        every { validationQueryRepository.findFindingsByConflictIds(listOf(1L)) } returns
            listOf(finding1, finding2)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(2, result.content[0].findings.size)
        assertEquals("rationale1", result.content[0].findings[0].rationale)
        assertEquals("rationale2", result.content[0].findings[1].rationale)
        assertEquals("suggestion1", result.content[0].findings[0].newText)
        assertEquals("suggestion2", result.content[0].findings[1].newText)
        assertEquals("ttl1", result.content[0].findings[0].title)
        assertEquals("ttl2", result.content[0].findings[1].title)
    }

    @Test
    fun `ignoredAt, ignoreReason이 ConflictResponse에 정확히 매핑`() {
        val edgeDetail = EdgeDetail(id = 10L, projectId = 1L, sourceDocumentId = 100L, targetDocumentId = 200L, validationCriterion = "criterion")
        val ignoredRow = ConflictRow(id = 1L, edgeId = 10L, ignoredAt = now, ignoreReason = "의도된 차이")
        val activeRow = ConflictRow(id = 2L, edgeId = 10L, ignoredAt = null, ignoreReason = null)

        every { searchEdgeDetailsByProject.search(1L) } returns listOf(edgeDetail)
        every { validationQueryRepository.findConflictsByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(ignoredRow, activeRow), PageRequest.of(0, 20), 2L)
        every { validationQueryRepository.findFindingsByConflictIds(listOf(1L, 2L)) } returns emptyList()

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(2, result.content.size)
        val ignored = result.content.first { it.id == 1L }
        assertEquals(now, ignored.ignoredAt)
        assertEquals("의도된 차이", ignored.ignoreReason)
        val active = result.content.first { it.id == 2L }
        assertEquals(null, active.ignoredAt)
    }
}
