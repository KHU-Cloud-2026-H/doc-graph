package com.docgraph.backend.notification.query.application

fun interface FindProjectWebhookQuery {
    fun find(projectId: Long, userId: Long): WebhookResponse?
}
