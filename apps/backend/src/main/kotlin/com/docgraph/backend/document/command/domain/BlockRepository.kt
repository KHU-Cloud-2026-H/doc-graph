package com.docgraph.backend.document.command.domain

import org.springframework.data.jpa.repository.JpaRepository

interface BlockRepository : JpaRepository<Block, Long> {

    fun findByDocument_IdOrderBySortOrderAsc(documentId: Long): List<Block>

    fun findByDocument_IdAndNotionBlockId(
        documentId: Long,
        notionBlockId: String,
    ): Block?

    fun deleteByDocument_Id(documentId: Long)
}
