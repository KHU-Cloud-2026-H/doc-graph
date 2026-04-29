package com.docgraph.backend.notification.command.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/projects")
@Tag(name = "Notification")
class NotificationCommandController {

    @PutMapping("/{id}/webhook")
    @Operation(summary = "프로젝트 Webhook URL 설정")
    fun updateWebhook(
        @PathVariable id: Long,
        @RequestBody request: UpdateWebhookRequest,
    ): ResponseEntity<Unit> {
        TODO()
    }
}

data class UpdateWebhookRequest(
    @Schema(description = "Slack 또는 Discord Incoming Webhook URL", example = "https://hooks.slack.com/services/xxx/yyy/zzz")
    val url: String,
)
