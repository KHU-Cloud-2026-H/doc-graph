package com.docgraph.backend.document.command.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Document")
class DocumentCommandController {

    @PostMapping("/notion")
    @Operation(summary = "Notion Webhook 수신 (page.content_updated / page.moved)")
    fun receiveWebhook(@RequestBody payload: NotionWebhookPayload): ResponseEntity<Unit> {
        TODO()
    }
}

data class NotionWebhookPayload(
    @Schema(description = "웹훅 이벤트 고유 ID")
    val id: String,
    @Schema(description = "이벤트 발생 시각 (ISO 8601)", example = "2024-12-05T19:49:36.997Z")
    val timestamp: String,
    @Schema(description = "이벤트 타입 (page.content_updated / page.moved)", example = "page.content_updated")
    val type: String,
    @Schema(description = "워크스페이스 ID")
    val workspaceId: String,
    @Schema(description = "웹훅 구독 ID")
    val subscriptionId: String,
    @Schema(description = "이벤트를 트리거한 엔티티")
    val entity: NotionEntity,
    @Schema(description = "이벤트 작성자 목록")
    val authors: List<NotionActor>,
    @Schema(description = "전달 시도 횟수 (최대 8회)", example = "1")
    val attemptNumber: Int,
)

data class NotionEntity(
    @Schema(description = "엔티티 ID (페이지 ID)", example = "abc1234567890def")
    val id: String,
    @Schema(description = "엔티티 타입", example = "page")
    val type: String,
)

data class NotionActor(
    @Schema(description = "actor ID", example = "c7c11cca-1d73-471d-9b6e-bdef51470190")
    val id: String,
    @Schema(description = "actor 타입 (person / bot)", example = "person")
    val type: String,
)
