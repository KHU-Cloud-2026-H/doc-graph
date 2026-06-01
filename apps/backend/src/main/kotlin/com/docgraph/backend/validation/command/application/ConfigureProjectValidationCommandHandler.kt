package com.docgraph.backend.validation.command.application

import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import com.docgraph.backend.validation.command.domain.ValidationPermissionDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ConfigureProjectValidationCommandHandler(
    private val settingRepository: ProjectValidationSettingRepository,
    private val searchAdminProjectIds: SearchAdminProjectIdsByUserIdQuery,
) {
    @Transactional
    fun handle(command: ConfigureProjectValidationCommand) {
        if (command.projectId !in searchAdminProjectIds.search(command.requesterUserId)) {
            throw ValidationPermissionDeniedException(command.projectId)
        }
        val now = OffsetDateTime.now()
        val existing = settingRepository.findByProjectId(command.projectId)
        if (existing != null) {
            existing.changeEnabled(command.enabled, now)
            settingRepository.save(existing)
        } else {
            settingRepository.save(
                ProjectValidationSetting(projectId = command.projectId, enabled = command.enabled, createdAt = now),
            )
        }
    }
}
