package com.docgraph.backend.acceptance

import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("acceptance")
class TestResetController(
    private val entityManager: EntityManager,
) {

    @PostMapping("/test/reset")
    @Transactional
    fun reset(): ResponseEntity<Void> {
        @Suppress("UNCHECKED_CAST")
        val tables = entityManager.createNativeQuery(
            """
            SELECT tablename FROM pg_tables
            WHERE schemaname = 'public'
              AND tablename != 'flyway_schema_history'
            """.trimIndent(),
        ).resultList as List<String>

        if (tables.isNotEmpty()) {
            val quoted = tables.joinToString(", ") { "\"$it\"" }
            entityManager.createNativeQuery("TRUNCATE TABLE $quoted CASCADE").executeUpdate()
        }
        return ResponseEntity.noContent().build()
    }
}
