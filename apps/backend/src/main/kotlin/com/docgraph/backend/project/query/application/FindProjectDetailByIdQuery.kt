package com.docgraph.backend.project.query.application

fun interface FindProjectDetailByIdQuery {
    fun find(projectId: Long, userId: Long): ProjectDetail?
}
