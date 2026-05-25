package com.docgraph.backend.project.query.application

import com.docgraph.backend.document.query.application.DocumentType

data class AssignedDocumentType(
    val projectId: Long,
    val documentType: DocumentType,
)
