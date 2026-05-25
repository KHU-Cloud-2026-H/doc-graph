package com.docgraph.backend.notification.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.notification.command.application.ConfigureProjectWebhookCommand
import com.docgraph.backend.notification.command.application.ConfigureProjectWebhookCommandHandler
import com.docgraph.backend.notification.command.domain.NotificationPermissionDeniedException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects")
@Tag(name = "Notification")
class NotificationCommandController(
    private val configureHandler: ConfigureProjectWebhookCommandHandler,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PutMapping("/{id}/webhook")
    @Operation(summary = "프로젝트 Webhook URL 설정")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "설정 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun updateWebhook(
        @PathVariable id: Long,
        @RequestBody request: UpdateWebhookRequest,
    ): ResponseEntity<Unit> {
        configureHandler.handle(
            ConfigureProjectWebhookCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                url = request.url,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(NotificationPermissionDeniedException::class)
    fun handleForbidden(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
}

data class UpdateWebhookRequest(
    @Schema(description = "Slack 또는 Discord Incoming Webhook URL", example = "https://hooks.slack.com/services/xxx/yyy/zzz")
    val url: String,
)
