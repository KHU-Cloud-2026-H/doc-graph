package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommand
import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommandHandler
import com.docgraph.backend.project.command.domain.ProjectSyncTriggeredEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProjectSyncTriggeredEventListener(
    private val handler: SyncProjectDocumentsCommandHandler,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectSyncTriggeredEvent) {
        try {
            handler.handle(
                SyncProjectDocumentsCommand(
                    projectId = event.projectId,
                    requestedBy = event.triggeredBy,
                ),
            )
        } catch (e: Exception) {
            // @Async라 예외가 호출자(이벤트 발행 트랜잭션)로 전파되지 않으므로 여기서 로그.
            logger.error(
                "프로젝트 동기화 실패 — projectId={} triggeredBy={}: {}",
                event.projectId, event.triggeredBy, e.message, e,
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectSyncTriggeredEventListener::class.java)
    }
}
