package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import com.docgraph.backend.project.command.domain.TypeAssigneeDefaultRepository
import org.springframework.stereotype.Component

@Component
class TypeAssigneeDefaultRepositoryImpl(
    private val jpa: TypeAssigneeDefaultJpaRepository,
) : TypeAssigneeDefaultRepository {
    override fun save(entity: TypeAssigneeDefault): TypeAssigneeDefault = jpa.save(entity)
    override fun saveAll(entities: List<TypeAssigneeDefault>): List<TypeAssigneeDefault> = jpa.saveAll(entities)
    override fun findAllByProjectId(projectId: Long): List<TypeAssigneeDefault> = jpa.findAllByProjectId(projectId)
    override fun deleteAllByProjectId(projectId: Long) {
        jpa.deleteAllByProjectId(projectId)
    }
}
