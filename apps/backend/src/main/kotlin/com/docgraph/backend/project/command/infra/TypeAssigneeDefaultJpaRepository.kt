package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TypeAssigneeDefaultJpaRepository : JpaRepository<TypeAssigneeDefault, Long> {
    fun findAllByProjectId(projectId: Long): List<TypeAssigneeDefault>

    // 벌크 DELETE로 즉시 실행. 파생 삭제(SELECT 후 em.remove)는 DELETE를 commit 시점으로 지연시키는데,
    // 엔티티가 IDENTITY 키라 뒤이은 saveAll의 INSERT가 즉시 실행되어 DELETE보다 먼저 DB에 도달 →
    // uk_type_assignee(project_id, document_type) 충돌. 벌크 DELETE는 호출 시점에 바로 반영되어 충돌을 없앤다.
    @Modifying
    @Query("delete from TypeAssigneeDefault t where t.projectId = :projectId")
    fun deleteAllByProjectId(@Param("projectId") projectId: Long)
}
