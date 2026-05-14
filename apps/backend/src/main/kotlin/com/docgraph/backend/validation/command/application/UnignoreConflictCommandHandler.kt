package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ConflictNotFoundException
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.IllegalConflictStateException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UnignoreConflictCommandHandler(
    private val conflictRepository: ConflictRepository,
) {
    @Transactional
    fun handle(command: UnignoreConflictCommand) {
        val conflict = conflictRepository.findById(command.conflictId)
            .orElseThrow { ConflictNotFoundException(command.conflictId) }
        if (!conflict.isActive) {
            throw IllegalConflictStateException(command.conflictId, "이미 해소된 충돌은 무시 해제 불가")
        }
        if (!conflict.isIgnored) {
            throw IllegalConflictStateException(command.conflictId, "무시 상태가 아님")
        }
        conflict.unignore()
    }
}
