package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.GetCurrentMemberQuery
import com.docgraph.backend.auth.query.application.GetCurrentMemberQueryHandler
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
class AcceptanceGetCurrentMemberQueryInactiveProfileTest @Autowired constructor(
    private val query: GetCurrentMemberQuery,
) {

    @Test
    fun `default profile에서 GetCurrentMemberQuery 빈은 운영 GetCurrentMemberQueryHandler`() {
        assertTrue(query is GetCurrentMemberQueryHandler)
    }
}