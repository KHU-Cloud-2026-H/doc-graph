package com.docgraph.backend.notification.query.interfaces.api

import com.docgraph.backend.notification.query.application.WebhookResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/projects")
@Tag(name = "Notification")
class NotificationQueryController {

    @GetMapping("/{id}/webhook")
    @Operation(summary = "프로젝트 Webhook URL 조회")
    fun getWebhook(@PathVariable id: Long): ResponseEntity<WebhookResponse> {
        TODO()
    }
}
