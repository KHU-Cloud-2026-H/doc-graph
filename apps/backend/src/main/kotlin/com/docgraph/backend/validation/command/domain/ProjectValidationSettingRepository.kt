package com.docgraph.backend.validation.command.domain

interface ProjectValidationSettingRepository {
    fun save(setting: ProjectValidationSetting): ProjectValidationSetting
    fun findByProjectId(projectId: Long): ProjectValidationSetting?
    fun deleteByProjectId(projectId: Long)
}
