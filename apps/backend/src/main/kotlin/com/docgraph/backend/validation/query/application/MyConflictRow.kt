package com.docgraph.backend.validation.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class MyConflictRow(
    @Schema(description = "충돌 ID", example = "1")
    val id: Long,
    @Schema(description = "연결된 엣지 ID", example = "1")
    val edgeId: Long,
    @Schema(description = "워크스페이스 ID", example = "1")
    val workspaceId: Long,
    @Schema(description = "프로젝트 ID", example = "1")
    val projectId: Long,
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val projectName: String,
    @Schema(description = "충돌 근거가 된 상대 문서 (의존 출발 문서)")
    val sourceDocument: InboxDocumentRef,
    @Schema(description = "담당 대상 문서 (의존 도착 문서)")
    val targetDocument: InboxDocumentRef,
    @Schema(description = "최신 finding의 한 줄 요약 제목", example = "결정사항 미반영")
    val title: String,
    @Schema(
        description = "최신 finding ID. conflict에 finding 다건이면 가장 최근 1건이며 title과 같은 finding. finding 없으면 null.",
        example = "1",
    )
    val latestFindingId: Long?,
    @Schema(
        description = "최신 finding의 AI 충돌 근거. title과 같은 finding이며, finding 없으면 null.",
        example = "회의록(결정사항)에서 ND_COUNT를 4로 변경하기로 결정하였으나, 설계서에는 2로 남아 있음.",
    )
    val rationale: String?,
    @Schema(description = "충돌 상태")
    val status: MyConflictStatus,
    @Schema(description = "최초 감지 시각")
    val firstDetectedAt: OffsetDateTime,
    @Schema(description = "무시 처리 시각 (미무시 시 null)")
    val ignoredAt: OffsetDateTime?,
)
