package com.docgraph.backend.workspace.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "workspace")
class Workspace(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "notion_workspace_id", nullable = false, length = 100)
    val notionWorkspaceId: String,

    @Column(name = "notion_workspace_name", nullable = false, length = 500)
    var notionWorkspaceName: String,

    @Column(name = "notion_bot_id", nullable = false, length = 100)
    val notionBotId: String,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
