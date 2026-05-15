package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.SearchEdgeDetailsByProjectQuery
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
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
        val edgeDetail = EdgeDetail(id = 10L, sourceDocumentId = 100L, targetDocumentId = 200L, validationCriterion = "criterion")
        val conflict = Conflict(id = 1L, edgeId = 10L, firstDetectedAt = now, lastDetectedAt = now)

        every { searchEdgeDetailsByProject.search(1L) } returns listOf(edgeDetail)
        every { validationQueryRepository.findConflictsByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(conflict), PageRequest.of(0, 20), 1L)
        every { validationQueryRepository.findFindingsByConflictIds(listOf(1L)) } returns emptyMap()

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(1, result.content.size)
        val response = result.content[0]
        assertEquals(10L, response.edgeId)
        assertEquals(100L, response.sourceDocumentId)
        assertEquals(200L, response.targetDocumentId)
    }

    @Test
    fun `findings가 conflictId별로 정확히 grouped되어 매핑`() {
        val edgeDetail = EdgeDetail(id = 10L, sourceDocumentId = 100L, targetDocumentId = 200L, validationCriterion = "criterion")
        val conflict = Conflict(id = 1L, edgeId = 10L, firstDetectedAt = now, lastDetectedAt = now)
        val finding1 = ConflictFinding(
            id = 11L, conflictId = 1L, validationTaskId = 99L,
            sourceBlockIds = listOf("a"), targetBlockIds = listOf("b"),
            rationale = "rationale1", detectedAt = now,
        )
        val finding2 = ConflictFinding(
            id = 12L, conflictId = 1L, validationTaskId = 99L,
            sourceBlockIds = listOf("c"), targetBlockIds = listOf("d"),
            rationale = "rationale2", detectedAt = now,
        )

        every { searchEdgeDetailsByProject.search(1L) } returns listOf(edgeDetail)
        every { validationQueryRepository.findConflictsByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(conflict), PageRequest.of(0, 20), 1L)
        every { validationQueryRepository.findFindingsByConflictIds(listOf(1L)) } returns
            mapOf(1L to listOf(finding1, finding2))

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(2, result.content[0].findings.size)
        assertEquals("rationale1", result.content[0].findings[0].rationale)
        assertEquals("rationale2", result.content[0].findings[1].rationale)
    }

    @Test
    fun `ignoredAt, ignoreReason이 ConflictResponse에 정확히 매핑`() {
        val edgeDetail = EdgeDetail(id = 10L, sourceDocumentId = 100L, targetDocumentId = 200L, validationCriterion = "criterion")
        val ignoredConflict = Conflict(
            id = 1L, edgeId = 10L, firstDetectedAt = now, lastDetectedAt = now,
            ignoredAt = now, ignoredBy = 42L, ignoreReason = "의도된 차이",
        )
        val activeConflict = Conflict(id = 2L, edgeId = 10L, firstDetectedAt = now, lastDetectedAt = now)

        every { searchEdgeDetailsByProject.search(1L) } returns listOf(edgeDetail)
        every { validationQueryRepository.findConflictsByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(ignoredConflict, activeConflict), PageRequest.of(0, 20), 2L)
        every { validationQueryRepository.findFindingsByConflictIds(listOf(1L, 2L)) } returns emptyMap()

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(2, result.content.size)
        val ignored = result.content.first { it.id == 1L }
        assertEquals(now, ignored.ignoredAt)
        assertEquals("의도된 차이", ignored.ignoreReason)
        val active = result.content.first { it.id == 2L }
        assertEquals(null, active.ignoredAt)
    }
}