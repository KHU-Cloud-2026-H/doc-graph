package com.docgraph.backend.document.command.application

import com.docgraph.backend.document.command.domain.BlockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 승인된 수정 제안이 Notion에 반영된 직후, 같은 텍스트로 로컬 블록 스냅샷을 맞춘다.
 * 봇 쓰기 webhook은 자동화 루프 방지를 위해 무시되므로, 이 경로가 없으면 프론트가 갱신을 못 본다.
 * 블록이 스냅샷에 없으면(미동기화 등) no-op.
 */
@Service
class ApplyApprovedBlockTextCommandHandler(
    private val blockRepository: BlockRepository,
) {
    @Transactional
    fun handle(command: ApplyApprovedBlockTextCommand) {
        val block = blockRepository.findByDocument_IdAndNotionBlockId(
            command.targetDocumentId,
            command.targetBlockId,
        ) ?: return
        block.applyApprovedText(command.newText)
        blockRepository.save(block)
    }
}
