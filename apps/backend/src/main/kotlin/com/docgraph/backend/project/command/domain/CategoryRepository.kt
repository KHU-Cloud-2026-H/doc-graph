package com.docgraph.backend.project.command.domain

interface CategoryRepository {
    fun save(category: Category): Category
    fun findById(id: Long): Category?
    fun findAllByProjectId(projectId: Long): List<Category>
    fun findByProjectIdAndNotionPageId(projectId: Long, notionPageId: String): Category?
    fun delete(category: Category)
}
