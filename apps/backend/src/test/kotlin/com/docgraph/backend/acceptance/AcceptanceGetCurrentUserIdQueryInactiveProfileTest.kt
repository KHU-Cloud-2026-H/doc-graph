package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.auth.query.application.GetCurrentUserIdQueryHandler
import com.docgraph.backend.fixtures.SharedPostgresContainer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import kotlin.test.assertTrue

@Tag("component")
@SpringBootTest
@Import(SharedPostgresContainer::class)
class AcceptanceGetCurrentUserIdQueryInactiveProfileTest @Autowired constructor(
    private val query: GetCurrentUserIdQuery,
) {

    @Test
    fun `default profile에서 GetCurrentUserIdQuery 빈은 운영 GetCurrentUserIdQueryHandler`() {
        assertTrue(query is GetCurrentUserIdQueryHandler)
    }
}