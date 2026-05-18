package com.docgraph.backend.project.command.application

import com.docgraph.backend.document.query.application.DocumentType

data class RegisterCategoryCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val notionPageId: String,
    val documentType: DocumentType,
)
