package com.docgraph.backend.notification.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "project_webhook")
class ProjectWebhook(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id", nullable = false, updatable = false)
    val projectId: Long,

    @Column(name = "url", nullable = false, columnDefinition = "text")
    var url: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = createdAt,
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
    }

    fun changeUrl(newUrl: String, at: OffsetDateTime) {
        require(newUrl.isNotBlank()) { "url must not be blank" }
        url = newUrl
        updatedAt = at
    }
}
