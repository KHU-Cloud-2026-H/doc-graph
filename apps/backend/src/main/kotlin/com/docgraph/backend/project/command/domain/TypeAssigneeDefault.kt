package com.docgraph.backend.project.command.domain

import com.docgraph.backend.document.query.application.DocumentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "type_assignee_default")
class TypeAssigneeDefault(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id", nullable = false)
    val projectId: Long,

    @Column(name = "document_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val documentType: DocumentType,

    @Column(name = "assignee_workspace_member_id")
    val assigneeWorkspaceMemberId: Long?,

    @Column(name = "assigned_at", nullable = false)
    val assignedAt: OffsetDateTime = OffsetDateTime.now(),
)
