package com.docgraph.backend.validation.query.application

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.SearchEdgeIdsByProjectQuery
import com.docgraph.backend.validation.command.domain.FailureCategory
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
class SearchValidationTasksByProjectQueryHandlerTest {

    private val searchEdgeIdsByProject = mockk<SearchEdgeIdsByProjectQuery>()
    private val validationQueryRepository = mockk<ValidationQueryRepository>()

    private val handler = SearchValidationTasksByProjectQueryHandler(
        searchEdgeIdsByProject,
        validationQueryRepository,
    )

    @Test
    fun `빈 edge list면 빈 PageResponse 반환`() {
        every { searchEdgeIdsByProject.search(1L) } returns emptyList()

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(0L, result.totalElements)
        assertTrue(result.content.isEmpty())
    }

    @Test
    fun `ValidationTask가 ValidationTaskResponse로 정확히 매핑`() {
        val createdAt = OffsetDateTime.now()
        val row = ValidationTaskRow(
            id = 5L, edgeId = 10L, status = OutboxStatus.PENDING, failureCategory = null, createdAt = createdAt,
        )

        every { searchEdgeIdsByProject.search(1L) } returns listOf(10L)
        every { validationQueryRepository.findValidationTasksByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(row), PageRequest.of(0, 20), 1L)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(1, result.content.size)
        val response = result.content[0]
        assertEquals(5L, response.id)
        assertEquals(10L, response.edgeId)
        assertEquals(ValidationStatus.PENDING, response.status)
        assertEquals(null, response.failureCategory)
        assertEquals(createdAt, response.createdAt)
    }

    @Test
    fun `OutboxStatus SUCCESS는 ValidationStatus SUCCESS로 매핑`() {
        val row = ValidationTaskRow(
            id = 6L, edgeId = 10L, status = OutboxStatus.SUCCESS, failureCategory = null, createdAt = OffsetDateTime.now(),
        )

        every { searchEdgeIdsByProject.search(1L) } returns listOf(10L)
        every { validationQueryRepository.findValidationTasksByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(row), PageRequest.of(0, 20), 1L)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(ValidationStatus.SUCCESS, result.content[0].status)
    }

    @Test
    fun `OutboxStatus FAILED는 ValidationStatus FAILED + failureCategory 그대로 노출`() {
        val row = ValidationTaskRow(
            id = 7L,
            edgeId = 10L,
            status = OutboxStatus.FAILED,
            failureCategory = FailureCategory.RATE_LIMITED,
            createdAt = OffsetDateTime.now(),
        )

        every { searchEdgeIdsByProject.search(1L) } returns listOf(10L)
        every { validationQueryRepository.findValidationTasksByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(row), PageRequest.of(0, 20), 1L)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(ValidationStatus.FAILED, result.content[0].status)
        assertEquals(FailureCategory.RATE_LIMITED, result.content[0].failureCategory)
    }
}
