package com.docgraph.backend.validation.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "project_validation_setting")
class ProjectValidationSetting(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id", nullable = false, updatable = false)
    val projectId: Long,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = createdAt,
) {
    fun changeEnabled(value: Boolean, at: OffsetDateTime) {
        enabled = value
        updatedAt = at
    }
}
