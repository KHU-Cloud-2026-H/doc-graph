package com.docgraph.backend.validation.query.application

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
    @Schema(description = "충돌 finding 목록")
    val findings: List<ConflictFindingResponse>,
    @Schema(description = "무시 처리 시각 (미무시 시 null)")
    val ignoredAt: OffsetDateTime?,
    @Schema(description = "무시 사유 (선택)", example = "의도된 차이로 확인됨")
    val ignoreReason: String?,
)