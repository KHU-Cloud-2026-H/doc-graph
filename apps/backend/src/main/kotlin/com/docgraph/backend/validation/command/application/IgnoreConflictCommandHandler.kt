package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ConflictNotFoundException
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.IllegalConflictStateException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class IgnoreConflictCommandHandler(
    private val conflictRepository: ConflictRepository,
) {
    @Transactional
    fun handle(command: IgnoreConflictCommand) {
        val conflict = conflictRepository.findById(command.conflictId)
            ?: throw ConflictNotFoundException(command.conflictId)
        if (!conflict.isActive) {
            throw IllegalConflictStateException(command.conflictId, "이미 해소된 충돌은 무시 불가")
        }
        if (conflict.isIgnored) {
            throw IllegalConflictStateException(command.conflictId, "이미 무시 상태")
        }
        conflict.ignore(by = command.ignoredBy, reason = command.reason, at = OffsetDateTime.now())
    }
}