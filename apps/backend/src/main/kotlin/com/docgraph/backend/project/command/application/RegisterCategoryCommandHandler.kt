package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.Category
import com.docgraph.backend.project.command.domain.CategoryDuplicateException
import com.docgraph.backend.project.command.domain.CategoryRepository
import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterCategoryCommandHandler(
    private val projectRepository: ProjectRepository,
    private val categoryRepository: CategoryRepository,
    private val authService: ProjectMemberAuthService,
) {
    @Transactional
    fun handle(command: RegisterCategoryCommand): Long {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        authService.requireAdminMember(project, command.requesterUserId)

        if (categoryRepository.findByProjectIdAndNotionPageId(project.id, command.notionPageId) != null) {
            throw CategoryDuplicateException(project.id, command.notionPageId)
        }

        return categoryRepository.save(
            Category(
                projectId = project.id,
                notionPageId = command.notionPageId,
                documentType = command.documentType,
            ),
        ).id
    }
}
