package com.docgraph.backend.notification.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class WebhookResponse(
    @Schema(description = "설정된 Webhook URL (미설정 시 null)", example = "https://hooks.slack.com/services/xxx/yyy/zzz")
    val url: String?,
)
