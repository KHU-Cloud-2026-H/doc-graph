package com.docgraph.backend.acceptance

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

/**
 * acceptance profile 한정 — UC4·UC5·UC8 spec이 검증 대상 외 conflict 상태를 seed하는 우회 진입점.
 * 운영 흐름은 ValidationTaskQueued → ProcessValidationTask → ConflictDetector → Conflict + Finding 영속.
 *
 * fixture는 ValidationTask(SUCCESS) + Conflict + Finding을 직접 영속.
 */
@RestController
@Profile("acceptance")
class ConflictFixtureController(
    private val validationTaskRepository: ValidationTaskRepository,
    private val conflictRepository: ConflictRepository,
    private val conflictFindingRepository: ConflictFindingRepository,
) {

    @PostMapping("/test/conflicts")
    @Transactional
    fun seedConflict(@RequestBody request: SeedConflictRequest): ResponseEntity<SeedConflictResponse> {
        val now = OffsetDateTime.now()

        val task = validationTaskRepository.save(
            ValidationTask(
                status = OutboxStatus.SUCCESS,
                attempts = 1,
                lastAttemptAt = now,
                validationPairId = UUID.randomUUID(),
                edgeId = request.edgeId,
            ),
        )

        val conflict = conflictRepository.save(
            Conflict(
                edgeId = request.edgeId,
                firstDetectedAt = now,
                lastDetectedAt = now,
            ),
        )

        val findings = request.findings.map { item ->
            conflictFindingRepository.save(
                ConflictFinding(
                    conflictId = conflict.id,
                    validationTaskId = task.id,
                    sourceBlockIds = item.sourceBlockIds,
                    targetBlockId = item.targetBlockId,
                    rationale = item.rationale,
                    newText = item.newText ?: "",
                    title = item.title ?: "",
                    detectedAt = now,
                ),
            )
        }

        return ResponseEntity.ok(
            SeedConflictResponse(
                id = conflict.id,
                findings = findings.map { FindingIdResponse(it.id) },
            ),
        )
    }
}

data class SeedConflictRequest(
    @Schema(example = "1")
    val edgeId: Long,
    val findings: List<SeedFindingItem>,
)

data class SeedFindingItem(
    val sourceBlockIds: List<String>,
    val targetBlockId: String,
    val rationale: String,
    val newText: String? = null,
    val title: String? = null,
)

data class SeedConflictResponse(
    val id: Long,
    val findings: List<FindingIdResponse>,
)

data class FindingIdResponse(
    val id: Long,
)
