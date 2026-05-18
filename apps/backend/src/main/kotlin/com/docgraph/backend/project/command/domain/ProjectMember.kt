package com.docgraph.backend.project.command.domain

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
@Table(name = "project_member")
class ProjectMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id", nullable = false)
    val projectId: Long,

    @Column(name = "workspace_member_id", nullable = false)
    val workspaceMemberId: Long,

    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var role: ProjectMemberRole,

    @Column(name = "assigned_at", nullable = false)
    val assignedAt: OffsetDateTime = OffsetDateTime.now(),
)
