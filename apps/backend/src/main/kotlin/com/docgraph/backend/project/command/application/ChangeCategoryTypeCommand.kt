package com.docgraph.backend.project.command.application

import com.docgraph.backend.document.query.application.DocumentType

data class ChangeCategoryTypeCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val categoryId: Long,
    val documentType: DocumentType,
)
