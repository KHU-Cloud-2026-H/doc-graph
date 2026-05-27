package com.docgraph.backend.project.query.application

data class ProjectRef(
    val id: Long,
    val workspaceId: Long,
    val name: String,
)
