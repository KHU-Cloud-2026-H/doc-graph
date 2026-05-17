package com.docgraph.backend.validation.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentMemberQuery
import com.docgraph.backend.validation.command.application.ApproveProposalCommand
import com.docgraph.backend.validation.command.application.ApproveProposalCommandHandler
import com.docgraph.backend.validation.command.application.IgnoreConflictCommand
import com.docgraph.backend.validation.command.application.IgnoreConflictCommandHandler
import com.docgraph.backend.validation.command.application.UnignoreConflictCommand
import com.docgraph.backend.validation.command.application.UnignoreConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictFindingNotFoundException
import com.docgraph.backend.validation.command.domain.ConflictNotFoundException
import com.docgraph.backend.validation.command.domain.IllegalConflictFindingStateException
import com.docgraph.backend.validation.command.domain.IllegalConflictStateException
import com.docgraph.backend.validation.command.domain.StaleProposalException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/conflicts")
@Tag(name = "Validation")
class ValidationCommandController(
    private val ignoreHandler: IgnoreConflictCommandHandler,
    private val unignoreHandler: UnignoreConflictCommandHandler,
    private val approveHandler: ApproveProposalCommandHandler,
    private val getCurrentMember: GetCurrentMemberQuery,
) {

    @PostMapping("/{id}/ignore")
    @Operation(
        summary = "충돌 수동 무시",
        description = "충돌을 무시 상태로 마킹한다. 무시된 충돌은 해당 문서 쌍 중 하나가 외부에서 변경되어 재검증이 실행되면 자동으로 해제된다.",
    )
    fun ignore(
        @PathVariable id: Long,
        @RequestBody request: IgnoreConflictRequest,
    ): ResponseEntity<Unit> {
        ignoreHandler.handle(
            IgnoreConflictCommand(
                conflictId = id,
                ignoredBy = getCurrentMember.get(),
                reason = request.reason,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/ignore")
    @Operation(summary = "충돌 무시 해제")
    fun unignore(@PathVariable id: Long): ResponseEntity<Unit> {
        unignoreHandler.handle(UnignoreConflictCommand(conflictId = id))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{conflictId}/findings/{findingId}/approve")
    @Operation(
        summary = "충돌 finding 수정 제안 승인",
        description = "AI가 생성한 수정 제안을 사용자가 승인한다. target 문서가 조회 시점 이후 변경되었으면 stale 409. 승인 시 ProposalApproved 이벤트를 발행하여 document 도메인이 Notion 쓰기를 처리한다.",
    )
    fun approve(
        @PathVariable conflictId: Long,
        @PathVariable findingId: Long,
        @RequestBody request: ApproveProposalRequest,
    ): ResponseEntity<Unit> {
        approveHandler.handle(
            ApproveProposalCommand(
                findingId = findingId,
                approvedBy = getCurrentMember.get(),
                expectedTargetNotionLastEditedAt = request.expectedTargetNotionLastEditedAt,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ConflictNotFoundException::class, ConflictFindingNotFoundException::class)
    fun handleNotFound(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    @ExceptionHandler(
        IllegalConflictStateException::class,
        IllegalConflictFindingStateException::class,
        StaleProposalException::class,
    )
    fun handleIllegalState(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.CONFLICT).build()
}

data class IgnoreConflictRequest(
    @Schema(description = "무시 사유 (선택)", example = "의도된 차이로 확인됨")
    val reason: String?,
)

data class ApproveProposalRequest(
    @Schema(description = "조회 시점에 본 target 문서의 notionLastEditedAt — server-side stale 비교에 사용 (target 측이 한 번도 동기화되지 않았다면 null)")
    val expectedTargetNotionLastEditedAt: OffsetDateTime?,
)
