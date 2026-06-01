package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardProjectValidationCommandHandler(
    private val settingRepository: ProjectValidationSettingRepository,
) {
    @Transactional
    fun handle(command: DiscardProjectValidationCommand) {
        settingRepository.deleteByProjectId(command.projectId)
    }
}
