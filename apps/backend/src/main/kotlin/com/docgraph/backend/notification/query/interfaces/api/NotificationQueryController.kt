package com.docgraph.backend.notification.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.notification.query.application.FindProjectWebhookQuery
import com.docgraph.backend.notification.query.application.WebhookResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects")
@Tag(name = "Notification")
class NotificationQueryController(
    private val findProjectWebhook: FindProjectWebhookQuery,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @GetMapping("/{id}/webhook")
    @Operation(summary = "프로젝트 Webhook URL 조회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 완료 — 미설정 시 url null"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun getWebhook(@PathVariable id: Long): ResponseEntity<WebhookResponse> {
        val response = findProjectWebhook.find(id, getCurrentUserId.get())
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(response)
    }
}
