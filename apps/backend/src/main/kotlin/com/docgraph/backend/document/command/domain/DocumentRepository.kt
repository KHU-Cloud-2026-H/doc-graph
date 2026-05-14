package com.docgraph.backend.document.command.domain

import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long> {

    fun findByProjectIdAndNotionPageId(
        projectId: Long,
        notionPageId: String,
    ): Document?

    fun findByProjectId(projectId: Long): List<Document>

    fun findByProjectIdAndParentNotionPageId(
        projectId: Long,
        parentNotionPageId: String,
    ): List<Document>
}
