package com.docgraph.backend.document.command.domain

import com.docgraph.backend.event.OutboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime

interface DocumentChangeNoticeRepository :
    JpaRepository<DocumentChangeNotice, Long>,
    OutboxRepository<DocumentChangeNotice> {

    @Query(
        """
        SELECT n FROM DocumentChangeNotice n
        WHERE n.status = 'PENDING'
          AND (n.lastAttemptAt IS NULL OR n.lastAttemptAt < :before)
        """,
    )
    override fun findStale(before: OffsetDateTime): List<DocumentChangeNotice>

    fun findByNotionEventId(notionEventId: String): DocumentChangeNotice?
}
