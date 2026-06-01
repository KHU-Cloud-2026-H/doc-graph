package com.docgraph.backend.validation.query.application

import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import org.springframework.stereotype.Service

@Service
class FindProjectValidationQueryHandler(
    private val settingRepository: ProjectValidationSettingRepository,
    private val searchAdminProjectIds: SearchAdminProjectIdsByUserIdQuery,
) : FindProjectValidationQuery {
    override fun find(projectId: Long, userId: Long): ProjectValidationResponse? {
        if (projectId !in searchAdminProjectIds.search(userId)) return null
        val enabled = settingRepository.findByProjectId(projectId)?.enabled ?: true
        return ProjectValidationResponse(enabled)
    }
}
