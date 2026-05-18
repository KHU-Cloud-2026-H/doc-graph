package com.docgraph.backend.project.query.application

data class ProjectRow(
    val id: Long,
    val name: String,
    val notionRootPageId: String,
)
