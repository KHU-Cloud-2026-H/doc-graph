package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import org.springframework.data.jpa.repository.JpaRepository

interface TypeAssigneeDefaultJpaRepository : JpaRepository<TypeAssigneeDefault, Long> {
    fun findAllByProjectId(projectId: Long): List<TypeAssigneeDefault>
    fun deleteAllByProjectId(projectId: Long)
}
