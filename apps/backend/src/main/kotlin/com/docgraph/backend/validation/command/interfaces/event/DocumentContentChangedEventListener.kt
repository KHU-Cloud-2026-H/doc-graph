package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.command.domain.DocumentContentChangedEvent
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.validation.command.application.EnqueueValidationTaskCommand
import com.docgraph.backend.validation.command.application.EnqueueValidationTaskCommandHandler
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DocumentContentChangedEventListener(
    private val edgeRepository: DependencyEdgeRepository,
    private val enqueueHandler: EnqueueValidationTaskCommandHandler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun on(event: DocumentContentChangedEvent) {
        log.debug("DocumentContentChangedEvent received: documentId={} projectId={}", event.documentId, event.projectId)
        val edges = edgeRepository.findAllBySourceDocumentId(event.documentId)
        log.debug("Found {} edges for documentId={}", edges.size, event.documentId)
        edges.forEach { edge ->
            log.debug("Enqueuing validation for edgeId={}", edge.id)
            enqueueHandler.handle(
                EnqueueValidationTaskCommand(
                    validationPairId = UUID.randomUUID(),
                    edgeId = edge.id,
                ),
            )
        }
    }
}
