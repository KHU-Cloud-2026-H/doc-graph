package com.docgraph.backend.validation.query.application

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.DocumentReference
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.SearchDocumentIdsByAssigneeQuery
import com.docgraph.backend.document.query.application.SearchDocumentIdsByProjectAndTypesQuery
import com.docgraph.backend.document.query.application.SearchDocumentReferencesByIdsQuery
import com.docgraph.backend.document.query.application.SearchUnassignedDocumentIdsByProjectQuery
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.SearchEdgeDetailsByIdsQuery
import com.docgraph.backend.graph.query.application.SearchEdgeIdsByTargetDocumentIdsQuery
import com.docgraph.backend.project.query.application.AssignedDocumentType
import com.docgraph.backend.project.query.application.ProjectRef
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.project.query.application.SearchAssignedDocumentTypesByUserIdQuery
import com.docgraph.backend.project.query.application.SearchProjectRefsByIdsQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import com.docgraph.backend.workspace.query.application.SearchWorkspaceMemberIdsByUserIdQuery
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
class SearchMyConflictsQueryHandlerTest {

    private val getCurrentUserId = mockk<GetCurrentUserIdQuery>()
    private val searchWorkspaceMemberIds = mockk<SearchWorkspaceMemberIdsByUserIdQuery>()
    private val searchDocumentIdsByAssignee = mockk<SearchDocumentIdsByAssigneeQuery>()
    private val searchAssignedDocumentTypes = mockk<SearchAssignedDocumentTypesByUserIdQuery>()
    private val searchDocumentIdsByProjectAndTypes = mockk<SearchDocumentIdsByProjectAndTypesQuery>()
    private val searchAdminProjectIds = mockk<SearchAdminProjectIdsByUserIdQuery>()
    private val searchUnassignedDocumentIds = mockk<SearchUnassignedDocumentIdsByProjectQuery>()
    private val searchDocumentReferences = mockk<SearchDocumentReferencesByIdsQuery>()
    private val searchEdgeIdsByTargetDocuments = mockk<SearchEdgeIdsByTargetDocumentIdsQuery>()
    private val searchEdgeDetailsByIds = mockk<SearchEdgeDetailsByIdsQuery>()
    private val searchProjectRefs = mockk<SearchProjectRefsByIdsQuery>()
    private val validationQueryRepository = mockk<ValidationQueryRepository>()

    private val handler = SearchMyConflictsQueryHandler(
        getCurrentUserId,
        searchWorkspaceMemberIds,
        searchDocumentIdsByAssignee,
        searchAssignedDocumentTypes,
        searchDocumentIdsByProjectAndTypes,
        searchAdminProjectIds,
        searchUnassignedDocumentIds,
        searchDocumentReferences,
        searchEdgeIdsByTargetDocuments,
        searchEdgeDetailsByIds,
        searchProjectRefs,
        validationQueryRepository,
    )

    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")

    @Test
    fun `정상 — 직접 담당 + type fallback + admin unassigned가 인박스에 조립`() {
        // user, workspace member
        every { getCurrentUserId.get() } returns 100L
        every { searchWorkspaceMemberIds.search(100L) } returns listOf(11L)
        // 직접 담당: target doc 20 (project 1)
        every { searchDocumentIdsByAssignee.search(11L) } returns listOf(20L)
        // type 매핑: target doc 21 (project 1, type planning)
        every { searchAssignedDocumentTypes.search(100L) } returns listOf(
            AssignedDocumentType(1L, DocumentType.PLANNING),
        )
        every { searchDocumentIdsByProjectAndTypes.search(any()) } returns listOf(21L)
        // admin: project 2의 unassigned doc 22
        every { searchAdminProjectIds.search(100L) } returns listOf(2L)
        every { searchUnassignedDocumentIds.search(2L) } returns listOf(22L)
        // target doc refs
        every { searchDocumentReferences.search(match { it.toSet() == setOf(20L, 21L, 22L) }) } returns listOf(
            DocumentReference(20L, 1L, "T20", DocumentType.REQUIREMENTS),
            DocumentReference(21L, 1L, "T21", DocumentType.PLANNING),
            DocumentReference(22L, 2L, "T22", null),
        )
        // edges: project 1 target [20,21] → edge 30; project 2 target [22] → edge 31
        every { searchEdgeIdsByTargetDocuments.search(1L, match { it.toSet() == setOf(20L, 21L) }) } returns listOf(30L)
        every { searchEdgeIdsByTargetDocuments.search(2L, listOf(22L)) } returns listOf(31L)
        // conflicts (active)
        every {
            validationQueryRepository.findInboxConflictsByEdgeIds(
                match { it.toSet() == setOf(30L, 31L) },
                MyConflictStatusFilter.ACTIVE,
                any(),
            )
        } returns PageImpl(
            listOf(
                InboxConflictRow(id = 500L, edgeId = 30L, firstDetectedAt = now, ignoredAt = null),
                InboxConflictRow(id = 501L, edgeId = 31L, firstDetectedAt = now, ignoredAt = null),
            ),
            PageRequest.of(0, 20),
            2L,
        )
        // edge details (source/target mapping)
        every { searchEdgeDetailsByIds.search(listOf(30L, 31L)) } returns listOf(
            EdgeDetail(id = 30L, sourceDocumentId = 40L, targetDocumentId = 20L, validationCriterion = "c1"),
            EdgeDetail(id = 31L, sourceDocumentId = 41L, targetDocumentId = 22L, validationCriterion = "c2"),
        )
        // source doc refs
        every { searchDocumentReferences.search(match { it.toSet() == setOf(40L, 41L) }) } returns listOf(
            DocumentReference(40L, 1L, "S40", DocumentType.PLANNING),
            DocumentReference(41L, 2L, "S41", DocumentType.REQUIREMENTS),
        )
        // project summaries
        every { searchProjectRefs.search(match { it.toSet() == setOf(1L, 2L) }) } returns listOf(
            ProjectRef(1L, "P1"),
            ProjectRef(2L, "P2"),
        )
        every { validationQueryRepository.findFindingsByConflictIds(match { it.toSet() == setOf(500L, 501L) }) } returns listOf(
            ConflictFindingRow(1L, 500L, listOf("x"), "y", "r", "n", "T500", now),
            ConflictFindingRow(2L, 501L, listOf("x"), "y", "r", "n", "T501", now),
        )

        val result = handler.search(MyConflictStatusFilter.ACTIVE, PageRequest.of(0, 20))

        assertEquals(2, result.content.size)
        val row1 = result.content.first { it.id == 500L }
        assertEquals(30L, row1.edgeId)
        assertEquals(1L, row1.projectId)
        assertEquals("P1", row1.projectName)
        assertEquals(InboxDocumentRef(40L, "S40", DocumentType.PLANNING), row1.sourceDocument)
        assertEquals(InboxDocumentRef(20L, "T20", DocumentType.REQUIREMENTS), row1.targetDocument)
        assertEquals(MyConflictStatus.ACTIVE, row1.status)
        assertEquals(now, row1.firstDetectedAt)
        assertEquals("T500", row1.title)

        val row2 = result.content.first { it.id == 501L }
        assertEquals(2L, row2.projectId)
        assertEquals("P2", row2.projectName)
        assertEquals(InboxDocumentRef(22L, "T22", null), row2.targetDocument)
        assertEquals("T501", row2.title)
    }

    @Test
    fun `IGNORED filter — conflict의 ignoredAt이 있으면 status IGNORED 매핑`() {
        every { getCurrentUserId.get() } returns 100L
        every { searchWorkspaceMemberIds.search(100L) } returns listOf(11L)
        every { searchDocumentIdsByAssignee.search(11L) } returns listOf(20L)
        every { searchAssignedDocumentTypes.search(100L) } returns emptyList()
        every { searchDocumentIdsByProjectAndTypes.search(emptyList()) } returns emptyList()
        every { searchAdminProjectIds.search(100L) } returns emptyList()
        every { searchDocumentReferences.search(listOf(20L)) } returns listOf(
            DocumentReference(20L, 1L, "T", DocumentType.REQUIREMENTS),
        )
        every { searchEdgeIdsByTargetDocuments.search(1L, listOf(20L)) } returns listOf(30L)
        val ignoredAt = OffsetDateTime.parse("2026-05-10T00:00:00Z")
        every {
            validationQueryRepository.findInboxConflictsByEdgeIds(listOf(30L), MyConflictStatusFilter.IGNORED, any())
        } returns PageImpl(
            listOf(InboxConflictRow(500L, 30L, now, ignoredAt)),
            PageRequest.of(0, 20),
            1L,
        )
        every { searchEdgeDetailsByIds.search(listOf(30L)) } returns listOf(
            EdgeDetail(30L, sourceDocumentId = 40L, targetDocumentId = 20L, validationCriterion = "c"),
        )
        every { searchDocumentReferences.search(listOf(40L)) } returns listOf(
            DocumentReference(40L, 1L, "S", DocumentType.PLANNING),
        )
        every { searchProjectRefs.search(listOf(1L)) } returns listOf(ProjectRef(1L, "P"))
        every { validationQueryRepository.findFindingsByConflictIds(listOf(500L)) } returns listOf(
            ConflictFindingRow(1L, 500L, listOf("x"), "y", "r", "n", "TI", now),
        )

        val result = handler.search(MyConflictStatusFilter.IGNORED, PageRequest.of(0, 20))

        assertEquals(MyConflictStatus.IGNORED, result.content[0].status)
        assertEquals(ignoredAt, result.content[0].ignoredAt)
    }

    @Test
    fun `assignee와 admin 모두 빈 결과면 빈 페이지 반환`() {
        every { getCurrentUserId.get() } returns 100L
        every { searchWorkspaceMemberIds.search(100L) } returns emptyList()
        every { searchAssignedDocumentTypes.search(100L) } returns emptyList()
        every { searchDocumentIdsByProjectAndTypes.search(emptyList()) } returns emptyList()
        every { searchAdminProjectIds.search(100L) } returns emptyList()

        val result = handler.search(MyConflictStatusFilter.ACTIVE, PageRequest.of(0, 20))

        assertTrue(result.content.isEmpty())
        assertEquals(0L, result.totalElements)
    }

    @Test
    fun `target docs는 있지만 edge가 없으면 빈 페이지`() {
        every { getCurrentUserId.get() } returns 100L
        every { searchWorkspaceMemberIds.search(100L) } returns listOf(11L)
        every { searchDocumentIdsByAssignee.search(11L) } returns listOf(20L)
        every { searchAssignedDocumentTypes.search(100L) } returns emptyList()
        every { searchDocumentIdsByProjectAndTypes.search(emptyList()) } returns emptyList()
        every { searchAdminProjectIds.search(100L) } returns emptyList()
        every { searchDocumentReferences.search(listOf(20L)) } returns listOf(
            DocumentReference(20L, 1L, "T", DocumentType.REQUIREMENTS),
        )
        every { searchEdgeIdsByTargetDocuments.search(1L, listOf(20L)) } returns emptyList()

        val result = handler.search(MyConflictStatusFilter.ACTIVE, PageRequest.of(0, 20))

        assertTrue(result.content.isEmpty())
        assertEquals(0L, result.totalElements)
    }
}
