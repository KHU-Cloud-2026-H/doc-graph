package com.docgraph.backend.document.command.application

import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.DocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardDocumentProjectCommandHandler(
    private val documentRepository: DocumentRepository,
    private val blockRepository: BlockRepository,
) {
    // DocumentProjectDiscardedEventListener(AFTER_COMMIT)에서 호출 — afterCommit 단계의 REQUIRED는 커밋되지 않으므로
    // REQUIRES_NEW로 독립 트랜잭션을 연다. (graph DiscardGraphProjectCommandHandler와 동일 이유)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: DiscardDocumentProjectCommand) {
        // FK(block.document_id → document) 때문에 block을 먼저 삭제한다.
        blockRepository.deleteByDocument_ProjectId(command.projectId)
        documentRepository.deleteByProjectId(command.projectId)
    }
}
