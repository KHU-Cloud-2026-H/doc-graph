package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryJpaRepository : JpaRepository<Category, Long> {
    fun findAllByProjectId(projectId: Long): List<Category>
    fun findByProjectIdAndNotionPageId(projectId: Long, notionPageId: String): Category?
}
