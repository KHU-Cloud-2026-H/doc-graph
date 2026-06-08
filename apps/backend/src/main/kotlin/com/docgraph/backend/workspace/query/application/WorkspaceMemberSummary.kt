package com.docgraph.backend.workspace.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class WorkspaceMemberSummary(
    @Schema(description = "워크스페이스 멤버 ID — 프로젝트 멤버 배정·타입별 담당자 지정 호출 시 사용", example = "1")
    val workspaceMemberId: Long,
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @Schema(description = "워크스페이스 가입 시점")
    val joinedAt: OffsetDateTime,
)