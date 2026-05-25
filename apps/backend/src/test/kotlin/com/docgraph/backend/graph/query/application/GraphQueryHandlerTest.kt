package com.docgraph.backend.graph.query.application

import com.docgraph.backend.document.query.application.DocumentNodeData
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.SearchDocumentNodesByProjectQuery
import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.EdgeConflictStatus
import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import com.docgraph.backend.graph.query.infra.GraphQueryRepository
import com.docgraph.backend.validation.query.application.ConflictStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class GraphQueryHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>()
    private val proposalRepository = mockk<EdgeProposalRepository>()
    private val ruleRepository = mockk<GraphRuleRepository>()
    private val searchDocumentNodes = mockk<SearchDocumentNodesByProjectQuery>()
    private val graphQueryRepository = mockk<GraphQueryRepository>()

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
    fun `SearchEdgeDetailsByIdsQueryHandler — id 묶음 기준 edge details 반환`() {
        every { edgeRepository.findAllByIds(listOf(100L, 101L)) } returns listOf(edge(id = 100L), edge(id = 101L))

        val result = SearchEdgeDetailsByIdsQueryHandler(edgeRepository).search(listOf(100L, 101L))

        assertEquals(listOf(100L, 101L), result.map { it.id })
        assertEquals(10L, result[0].sourceDocumentId)
        assertEquals(20L, result[0].targetDocumentId)
    }

    @Test
    fun `SearchEdgeDetailsByIdsQueryHandler — 빈 입력이면 빈 리스트`() {
        every { edgeRepository.findAllByIds(emptyList()) } returns emptyList()
        val result = SearchEdgeDetailsByIdsQueryHandler(edgeRepository).search(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `SearchEdgeIdsByTargetDocumentIdsQueryHandler — target 문서 기준 edge ids 반환`() {
        every { edgeRepository.findAllByProjectIdAndTargetDocumentIdIn(1L, listOf(20L, 21L)) } returns
            listOf(edge(id = 100L), edge(id = 101L))

        val result = SearchEdgeIdsByTargetDocumentIdsQueryHandler(edgeRepository).search(1L, listOf(20L, 21L))

        assertEquals(listOf(100L, 101L), result)
    }

    @Test
    fun `SearchEdgeIdsByProjectIdsQueryHandler — Repository 결과를 그대로 반환`() {
        every { graphQueryRepository.findEdgeIdsByProjectIdIn(listOf(1L, 2L)) } returns mapOf(
            1L to listOf(100L, 101L),
            2L to listOf(102L),
        )

        val result = SearchEdgeIdsByProjectIdsQueryHandler(graphQueryRepository).search(listOf(1L, 2L))

        assertEquals(listOf(100L, 101L), result[1L])
        assertEquals(listOf(102L), result[2L])
    }

    @Test
    fun `SearchEdgesByProjectQueryHandler — EdgeResponse 매핑 + conflictStatus 변환`() {
        val edgeNone = edge(id = 100L)
        val edgeConflict = edge(id = 101L).apply { markConflict() }
        every { edgeRepository.findAllByProjectId(1L) } returns listOf(edgeNone, edgeConflict)

        val result = SearchEdgesByProjectQueryHandler(edgeRepository).search(1L)

        assertEquals(listOf(100L, 101L), result.map { it.id })
        assertEquals(ConflictStatus.NONE, result[0].conflictStatus)
        assertEquals(ConflictStatus.CONFLICT, result[1].conflictStatus)
        assertEquals(DependencyEdgeSource.NOTION_REFERENCE, result[0].source)
    }

    @Test
    fun `SearchEdgeProposalsByProjectQueryHandler — EdgeProposalResponse 매핑`() {
        every { proposalRepository.findAllByProjectId(1L) } returns listOf(proposal(id = 200L), proposal(id = 201L))

        val result = SearchEdgeProposalsByProjectQueryHandler(proposalRepository).search(1L)

        assertEquals(listOf(200L, 201L), result.map { it.id })
        assertEquals(0.87, result[0].similarityScore)
        assertEquals("범위 일치 여부", result[0].validationCriterion)
    }

    @Test
    fun `SearchGraphRulesByProjectQueryHandler — 기본 + 커스텀 룰 매핑`() {
        val defaultRule = rule(id = 300L, isDefault = true, projectId = null)
        val customRule = rule(id = 301L, isDefault = false, projectId = 1L)
        every { ruleRepository.findAllApplicableToProject(1L) } returns listOf(defaultRule, customRule)

        val result = SearchGraphRulesByProjectQueryHandler(ruleRepository).search(1L)

        assertEquals(listOf(300L, 301L), result.map { it.id })
        assertEquals(true, result[0].isDefault)
        assertNull(result[0].projectId)
        assertEquals(false, result[1].isDefault)
        assertEquals(1L, result[1].projectId)
    }

    @Test
    fun `FindProjectGraphQueryHandler — nodes·edges·proposals 묶음 반환`() {
        every { searchDocumentNodes.search(1L) } returns listOf(
            DocumentNodeData(id = 10L, title = "기획서", type = DocumentType.PLANNING, assigneeMemberId = 1L),
            DocumentNodeData(id = 20L, title = "요구사항", type = DocumentType.REQUIREMENTS, assigneeMemberId = null),
        )
        every { edgeRepository.findAllByProjectId(1L) } returns listOf(edge(id = 100L))
        every { proposalRepository.findAllByProjectId(1L) } returns listOf(proposal(id = 200L))

        val result = FindProjectGraphQueryHandler(edgeRepository, proposalRepository, searchDocumentNodes).find(1L)

        assertEquals(listOf(10L, 20L), result.nodes.map { it.id })
        assertEquals("기획서", result.nodes[0].title)
        assertEquals(DocumentType.PLANNING, result.nodes[0].type)
        assertEquals(1L, result.nodes[0].assigneeMemberId)
        assertNull(result.nodes[1].assigneeMemberId)
        assertEquals(listOf(100L), result.edges.map { it.id })
        assertEquals(listOf(200L), result.proposals.map { it.id })
    }

    private fun edge(id: Long, projectId: Long = 1L): DependencyEdge =
        DependencyEdge(
            id = id,
            projectId = projectId,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
        )

    private fun proposal(id: Long): EdgeProposal =
        EdgeProposal(
            id = id,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            similarityScore = 0.87,
        )

    private fun rule(id: Long, isDefault: Boolean, projectId: Long?): GraphRule =
        GraphRule(
            id = id,
            projectId = projectId,
            sourceType = DocumentType.PLANNING,
            targetType = DocumentType.REQUIREMENTS,
            validationCriterion = "범위 일치 여부",
            isDefault = isDefault,
        )
}
