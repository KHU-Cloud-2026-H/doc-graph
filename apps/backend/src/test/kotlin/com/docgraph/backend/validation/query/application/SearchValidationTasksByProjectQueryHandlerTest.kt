package com.docgraph.backend.validation.query.application

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.SearchEdgeIdsByProjectQuery
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.util.UUID
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
        val task = ValidationTask(
            id = 5L,
            validationPairId = UUID.randomUUID(),
            edgeId = 10L,
            status = OutboxStatus.PENDING,
            createdAt = createdAt,
        )

        every { searchEdgeIdsByProject.search(1L) } returns listOf(10L)
        every { validationQueryRepository.findValidationTasksByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(task), PageRequest.of(0, 20), 1L)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(1, result.content.size)
        val response = result.content[0]
        assertEquals(5L, response.id)
        assertEquals(10L, response.edgeId)
        assertEquals(ValidationStatus.PENDING, response.status)
        assertEquals(createdAt, response.createdAt)
    }

    @Test
    fun `OutboxStatus SUCCESS는 ValidationStatus SUCCESS로 매핑`() {
        val task = ValidationTask(
            id = 6L, validationPairId = UUID.randomUUID(), edgeId = 10L,
            status = OutboxStatus.SUCCESS,
        )

        every { searchEdgeIdsByProject.search(1L) } returns listOf(10L)
        every { validationQueryRepository.findValidationTasksByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(task), PageRequest.of(0, 20), 1L)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(ValidationStatus.SUCCESS, result.content[0].status)
    }

    @Test
    fun `OutboxStatus FAILED는 ValidationStatus FAILED로 매핑`() {
        val task = ValidationTask(
            id = 7L, validationPairId = UUID.randomUUID(), edgeId = 10L,
            status = OutboxStatus.FAILED,
        )

        every { searchEdgeIdsByProject.search(1L) } returns listOf(10L)
        every { validationQueryRepository.findValidationTasksByEdgeIds(listOf(10L), any()) } returns
            PageImpl(listOf(task), PageRequest.of(0, 20), 1L)

        val result = handler.search(1L, PageRequest.of(0, 20))

        assertEquals(ValidationStatus.FAILED, result.content[0].status)
    }
}