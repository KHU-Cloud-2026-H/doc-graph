package com.docgraph.backend.validation.query.application

fun interface FindProjectValidationQuery {
    fun find(projectId: Long, userId: Long): ProjectValidationResponse?
}
