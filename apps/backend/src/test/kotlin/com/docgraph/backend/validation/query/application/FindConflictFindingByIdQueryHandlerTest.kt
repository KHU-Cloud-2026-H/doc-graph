package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("unit")
class FindConflictFindingByIdQueryHandlerTest {

    private val validationQueryRepository = mockk<ValidationQueryRepository>()
    private val findEdgeById = mockk<FindEdgeByIdQuery>()

    private val handler = FindConflictFindingByIdQueryHandler(
        validationQueryRepository,
        findEdgeById,
    )

    @Test
    fun `정상 — finding row + edge 조회 후 targetDocumentId 보강한 ConflictFindingDetail 반환`() {
        val row = ConflictFindingDetailRow(
            findingId = 50L,
            edgeId = 70L,
            targetBlockId = "block-t",
            newText = "교체 텍스트",
        )
        every { validationQueryRepository.findFindingDetailById(50L) } returns row
        every { findEdgeById.find(70L) } returns EdgeDetail(
            id = 70L,
            sourceDocumentId = 100L,
            targetDocumentId = 200L,
            validationCriterion = "c",
        )

        val result = handler.find(50L)

        assertEquals(
            ConflictFindingDetail(
                findingId = 50L,
                targetDocumentId = 200L,
                targetBlockId = "block-t",
                newText = "교체 텍스트",
            ),
            result,
        )
    }

    @Test
    fun `finding 없음 — null 반환 (edge 조회 안 함)`() {
        every { validationQueryRepository.findFindingDetailById(404L) } returns null

        assertNull(handler.find(404L))
    }

    @Test
    fun `finding은 있는데 edge 정합성 결손 — IllegalStateException`() {
        val row = ConflictFindingDetailRow(
            findingId = 51L,
            edgeId = 71L,
            targetBlockId = "b",
            newText = "n",
        )
        every { validationQueryRepository.findFindingDetailById(51L) } returns row
        every { findEdgeById.find(71L) } returns null

        assertThrows<IllegalStateException> { handler.find(51L) }
    }
}
