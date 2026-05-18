package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.Category
import com.docgraph.backend.project.command.domain.CategoryRepository
import org.springframework.stereotype.Component

@Component
class CategoryRepositoryImpl(
    private val jpa: CategoryJpaRepository,
) : CategoryRepository {
    override fun save(category: Category): Category = jpa.save(category)
    override fun findById(id: Long): Category? = jpa.findById(id).orElse(null)
    override fun findAllByProjectId(projectId: Long): List<Category> = jpa.findAllByProjectId(projectId)
    override fun findByProjectIdAndNotionPageId(projectId: Long, notionPageId: String): Category? =
        jpa.findByProjectIdAndNotionPageId(projectId, notionPageId)
    override fun delete(category: Category) = jpa.delete(category)
}
