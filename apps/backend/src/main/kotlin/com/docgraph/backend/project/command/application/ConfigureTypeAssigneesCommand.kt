package com.docgraph.backend.project.command.application

import com.docgraph.backend.document.query.application.DocumentType

data class ConfigureTypeAssigneesCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val assignments: List<TypeAssigneeAssignment>,
)

data class TypeAssigneeAssignment(
    val documentType: DocumentType,
    val assigneeWorkspaceMemberId: Long?,
)
