package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import org.springframework.stereotype.Component

@Component
class ProjectValidationSettingRepositoryImpl(
    private val jpa: ProjectValidationSettingJpaRepository,
) : ProjectValidationSettingRepository {
    override fun save(setting: ProjectValidationSetting): ProjectValidationSetting = jpa.save(setting)
    override fun findByProjectId(projectId: Long): ProjectValidationSetting? = jpa.findByProjectId(projectId)
    override fun deleteByProjectId(projectId: Long) = jpa.deleteByProjectId(projectId)
}
