package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectValidationSettingJpaRepository : JpaRepository<ProjectValidationSetting, Long> {
    fun findByProjectId(projectId: Long): ProjectValidationSetting?
    fun deleteByProjectId(projectId: Long)
}