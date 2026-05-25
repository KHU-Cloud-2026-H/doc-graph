package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.CategoryNotFoundException
import com.docgraph.backend.project.command.domain.CategoryRepository
import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveCategoryCommandHandler(
    private val projectRepository: ProjectRepository,
    private val categoryRepository: CategoryRepository,
    private val authService: ProjectMemberAuthService,
) {
    @Transactional
    fun handle(command: RemoveCategoryCommand) {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        authService.requireAdminMember(project, command.requesterUserId)

        val category = categoryRepository.findById(command.categoryId)
            ?: throw CategoryNotFoundException(command.projectId, command.categoryId)
        if (category.projectId != command.projectId) {
            throw CategoryNotFoundException(command.projectId, command.categoryId)
        }

        categoryRepository.delete(category)
    }
}
