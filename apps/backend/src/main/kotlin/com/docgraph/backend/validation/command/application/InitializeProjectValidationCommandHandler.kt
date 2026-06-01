package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class InitializeProjectValidationCommandHandler(
    private val settingRepository: ProjectValidationSettingRepository,
) {
    @Transactional
    fun handle(command: InitializeProjectValidationCommand) {
        settingRepository.save(
            ProjectValidationSetting(
                projectId = command.projectId,
                enabled = command.enabled,
                createdAt = OffsetDateTime.now(),
            ),
        )
    }
}
