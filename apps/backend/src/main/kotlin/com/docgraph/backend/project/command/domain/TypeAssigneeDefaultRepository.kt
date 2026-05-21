package com.docgraph.backend.project.command.domain

interface TypeAssigneeDefaultRepository {
    fun save(entity: TypeAssigneeDefault): TypeAssigneeDefault
    fun saveAll(entities: List<TypeAssigneeDefault>): List<TypeAssigneeDefault>
    fun findAllByProjectId(projectId: Long): List<TypeAssigneeDefault>
    fun deleteAllByProjectId(projectId: Long)
}
