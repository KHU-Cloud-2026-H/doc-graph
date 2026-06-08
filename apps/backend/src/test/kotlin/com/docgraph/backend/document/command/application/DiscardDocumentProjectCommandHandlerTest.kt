package com.docgraph.backend.document.command.application

import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.DocumentRepository
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class DiscardDocumentProjectCommandHandlerTest {

    private val documentRepository = mockk<DocumentRepository>(relaxUnitFun = true)
    private val blockRepository = mockk<BlockRepository>(relaxUnitFun = true)
    private val handler = DiscardDocumentProjectCommandHandler(documentRepository, blockRepository)

    @Test
    fun `handle — 프로젝트 소속 block을 먼저, 그 다음 document를 삭제한다`() {
        handler.handle(DiscardDocumentProjectCommand(projectId = 1L))

        // FK(block.document_id → document) 때문에 block이 반드시 document보다 먼저 삭제돼야 한다.
        verifyOrder {
            blockRepository.deleteByDocument_ProjectId(1L)
            documentRepository.deleteByProjectId(1L)
        }
    }
}
