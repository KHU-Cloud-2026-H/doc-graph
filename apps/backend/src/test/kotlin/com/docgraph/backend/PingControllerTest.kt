package com.docgraph.backend

import com.docgraph.backend.fixtures.SharedPostgresContainer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedPostgresContainer::class)
class PingControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `GET ping — 200, pong`() {
        mockMvc.get("/ping").andExpect {
            status { isOk() }
            content { string("pong") }
        }
    }
}