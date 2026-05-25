package com.docgraph.backend.project.command.application

data class RemoveCategoryCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val categoryId: Long,
)
