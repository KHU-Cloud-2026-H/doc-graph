package com.docgraph.backend.document.command.interfaces.scheduler

import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeQueuedEvent
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.event.AbstractStalePendingScheduler
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DocumentChangeNoticeStaleScheduler(
    repository: DocumentChangeNoticeRepository,
    publisher: ApplicationEventPublisher,
) : AbstractStalePendingScheduler<DocumentChangeNotice>(repository, publisher) {

    override fun rehydrate(entry: DocumentChangeNotice): Any =
        DocumentChangeNoticeQueuedEvent(entry.id)

    @Scheduled(fixedRate = INTERVAL_MS)
    fun trigger() {
        processStaleRows(THRESHOLD)
    }

    companion object {
        private val THRESHOLD: Duration = Duration.ofMinutes(5)
        private const val INTERVAL_MS: Long = 5L * 60 * 1000
    }
}
