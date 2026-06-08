package com.docgraph.backend.validation.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import jakarta.persistence.EntityManager
import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime
import java.util.UUID

class FakeGetCurrentUserIdQuery : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 7L
    override fun get(): Long = userId
}

class FakeFindEdgeByIdQuery : FindEdgeByIdQuery {
    @Volatile var behavior: (Long) -> EdgeDetail? = { null }
    override fun find(edgeId: Long): EdgeDetail? = behavior(edgeId)
}

class FakeFindDocumentByIdQuery : FindDocumentByIdQuery {
    @Volatile var behavior: (Long) -> DocumentDetail? = { null }
    override fun find(documentId: Long): DocumentDetail? = behavior(documentId)
}

class ProposalApprovedProbe {
    val received: MutableList<ProposalApprovedEvent> = mutableListOf()

    @EventListener
    fun on(event: ProposalApprovedEvent) {
        received += event
    }
}

@TestConfiguration
class ValidationCommandControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeFindEdgeById() = FakeFindEdgeByIdQuery()
    @Bean @Primary fun fakeFindDocumentById() = FakeFindDocumentByIdQuery()
    @Bean fun proposalApprovedProbe() = ProposalApprovedProbe()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ValidationCommandControllerTestConfig::class, SharedPostgresContainer::class)
class ValidationCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val conflictRepository: ConflictRepository,
    private val findingRepository: ConflictFindingRepository,
    private val taskRepository: ValidationTaskRepository,
    private val em: EntityManager,
    private val txTemplate: TransactionTemplate,
    private val getCurrentUserId: FakeGetCurrentUserIdQuery,
    private val findEdge: FakeFindEdgeByIdQuery,
    private val findDocument: FakeFindDocumentByIdQuery,
    private val proposalApprovedProbe: ProposalApprovedProbe,
) {

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM ConflictFinding").executeUpdate()
            em.createQuery("DELETE FROM Conflict").executeUpdate()
            em.createQuery("DELETE FROM ValidationTask").executeUpdate()
        }
        getCurrentUserId.userId = 7L
        findEdge.behavior = { null }
        findDocument.behavior = { null }
        proposalApprovedProbe.received.clear()
    }

    @Test
    fun `POST ignore — 활성 충돌 → 204, DB에 ignored 상태`() {
        val conflict = save(newActive(edgeId = 51L))

        mockMvc.post("/conflicts/${conflict.id}/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest("의도된 차이"))
        }.andExpect {
            status { isNoContent() }
        }

        val reloaded = conflictRepository.findById(conflict.id)!!
        assert(reloaded.ignoredAt != null)
        assert(reloaded.ignoredBy == 7L)
        assert(reloaded.ignoreReason == "의도된 차이")
    }

    @Test
    fun `POST ignore — 이미 무시 상태 → 409`() {
        val conflict = save(
            newActive(edgeId = 52L).apply {
                ignore(by = 1L, reason = "기존 무시", at = OffsetDateTime.now())
            },
        )

        mockMvc.post("/conflicts/${conflict.id}/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest(null))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST ignore — 이미 해소된 충돌 → 409`() {
        val conflict = save(
            newActive(edgeId = 53L).apply {
                markResolved(OffsetDateTime.now())
            },
        )

        mockMvc.post("/conflicts/${conflict.id}/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest(null))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST ignore — 존재 X → 404`() {
        mockMvc.post("/conflicts/9999/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest(null))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE ignore — 무시 상태 → 204, DB에서 ignored 해제`() {
        val conflict = save(
            newActive(edgeId = 54L).apply {
                ignore(by = 1L, reason = "테스트", at = OffsetDateTime.now())
            },
        )

        mockMvc.delete("/conflicts/${conflict.id}/ignore").andExpect {
            status { isNoContent() }
        }

        val reloaded = conflictRepository.findById(conflict.id)!!
        assert(reloaded.ignoredAt == null)
        assert(reloaded.ignoredBy == null)
        assert(reloaded.ignoreReason == null)
    }

    @Test
    fun `DELETE ignore — 무시 상태 아님 → 409`() {
        val conflict = save(newActive(edgeId = 55L))

        mockMvc.delete("/conflicts/${conflict.id}/ignore").andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `DELETE ignore — 존재 X → 404`() {
        mockMvc.delete("/conflicts/9999/ignore").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST approve — happy, finding approved + ProposalApprovedEvent 발행 + 204`() {
        val edited = OffsetDateTime.parse("2026-05-17T10:00:00Z")
        val fixture = approveFixture(edgeId = 61L, targetDocumentId = 161L, targetEdited = edited)

        mockMvc.post("/conflicts/${fixture.conflict.id}/findings/${fixture.finding.id}/approve") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ApproveProposalRequest(edited))
        }.andExpect {
            status { isNoContent() }
        }

        val reloaded = findingRepository.findById(fixture.finding.id)!!
        assert(reloaded.isApproved)
        assert(reloaded.approvedBy == 7L)
        assert(proposalApprovedProbe.received.size == 1)
        assert(proposalApprovedProbe.received.single().conflictFindingId == fixture.finding.id)
    }

    @Test
    fun `POST approve — finding 없음 → 404`() {
        mockMvc.post("/conflicts/9999/findings/9999/approve") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ApproveProposalRequest(null))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST approve — 이미 승인됨 → 409`() {
        val edited = OffsetDateTime.parse("2026-05-17T10:00:00Z")
        val fixture = approveFixture(edgeId = 62L, targetDocumentId = 162L, targetEdited = edited).also {
            it.finding.approve(by = 1L, at = OffsetDateTime.now())
            findingRepository.save(it.finding)
        }

        mockMvc.post("/conflicts/${fixture.conflict.id}/findings/${fixture.finding.id}/approve") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ApproveProposalRequest(edited))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST approve — target 사이에 변경 (stale) → 409`() {
        val edited = OffsetDateTime.parse("2026-05-17T10:00:00Z")
        val fixture = approveFixture(edgeId = 63L, targetDocumentId = 163L, targetEdited = OffsetDateTime.parse("2026-05-17T11:00:00Z"))

        mockMvc.post("/conflicts/${fixture.conflict.id}/findings/${fixture.finding.id}/approve") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ApproveProposalRequest(edited))
        }.andExpect {
            status { isConflict() }
        }
    }

    private fun newActive(edgeId: Long): Conflict {
        val now = OffsetDateTime.now()
        return Conflict(edgeId = edgeId, firstDetectedAt = now, lastDetectedAt = now)
    }

    private fun save(conflict: Conflict): Conflict = conflictRepository.save(conflict)
    private data class ApproveFixture(val conflict: Conflict, val finding: ConflictFinding)

    private fun approveFixture(
        edgeId: Long,
        targetDocumentId: Long,
        targetEdited: OffsetDateTime?,
    ): ApproveFixture {
        val task = taskRepository.save(
            ValidationTask(validationPairId = UUID.randomUUID(), edgeId = edgeId),
        )
        val conflict = save(newActive(edgeId))
        val finding = findingRepository.save(
            ConflictFinding(
                conflictId = conflict.id,
                validationTaskId = task.id,
                sourceBlockIds = listOf("s"),
                targetBlockId = "t",
                rationale = "r",
                newText = "sug",
                title = "tt",
                detectedAt = OffsetDateTime.now(),
            ),
        )
        findEdge.behavior = { id ->
            if (id == edgeId) EdgeDetail(edgeId, 1L, 100L, targetDocumentId, "c") else null
        }
        findDocument.behavior = { id ->
            if (id == targetDocumentId) {
                DocumentDetail(id, "page-$id", "t-$id", DocumentType.REQUIREMENTS, null, null, null, targetEdited, null, null, emptyList())
            } else null
        }
        return ApproveFixture(conflict, finding)
    }
}