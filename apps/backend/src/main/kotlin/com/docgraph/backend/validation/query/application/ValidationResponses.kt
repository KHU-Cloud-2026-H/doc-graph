package com.docgraph.backend.validation.query.application

import com.docgraph.backend.common.ValidationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class ConflictResponse(
    @Schema(description = "충돌 ID", example = "1")
    val id: Long,
    @Schema(description = "연결된 엣지 ID", example = "1")
    val edgeId: Long,
    @Schema(description = "출발 문서 ID", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "충돌 구간 (AI가 식별한 원문 발췌)", example = "3.2절 범위 정의 항목")
    val conflictSection: String,
    @Schema(description = "충돌 원인", example = "planning의 범위가 requirements에 반영되지 않음")
    val cause: String,
    @Schema(description = "수정 제안", example = "requirements 3.2절에 해당 기능 범위 추가 필요")
    val suggestion: String,
    @Schema(description = "무시 처리 시각 (미무시 시 null)")
    val ignoredAt: OffsetDateTime?,
    @Schema(description = "무시 사유 (선택)", example = "의도된 차이로 확인됨")
    val ignoreReason: String?,
)

data class ValidationRunResponse(
    @Schema(description = "검증 실행 ID", example = "1")
    val id: Long,
    @Schema(description = "검증 대상 엣지 ID", example = "1")
    val edgeId: Long,
    @Schema(description = "검증 상태")
    val status: ValidationStatus,
    @Schema(description = "실행 시각")
    val createdAt: OffsetDateTime,
)
