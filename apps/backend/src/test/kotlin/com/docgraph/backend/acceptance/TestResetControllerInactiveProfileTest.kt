package com.docgraph.backend.acceptance

import com.docgraph.backend.fixtures.SharedPostgresContainer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedPostgresContainer::class)
class TestResetControllerInactiveProfileTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `POST test reset — acceptance profile 미활성 → 404`() {
        mockMvc.post("/test/reset").andExpect {
            status { isNotFound() }
        }
    }
}