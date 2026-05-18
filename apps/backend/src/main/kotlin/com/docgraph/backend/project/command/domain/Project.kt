package com.docgraph.backend.project.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "project")
class Project(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "name", nullable = false, length = 500)
    var name: String,

    @Column(name = "notion_root_page_id", nullable = false, length = 100)
    val notionRootPageId: String,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
