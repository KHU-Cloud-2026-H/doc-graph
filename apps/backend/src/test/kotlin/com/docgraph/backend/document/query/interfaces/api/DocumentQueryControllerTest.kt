package com.docgraph.backend.document.query.interfaces.api

import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.query.application.IconType
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
class DocumentQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val documentRepository: DocumentRepository,
) {

    @Test
    fun `GET documents detail — icon 컬럼 노출`() {
        val doc = documentRepository.save(
            Document(
                projectId = 1L,
                notionPageId = "page-icon-${System.nanoTime()}",
                title = "Doc X",
                iconType = IconType.EXTERNAL,
                iconValue = "https://x/i.png",
            ),
        )

        mockMvc.get("/documents/${doc.id}").andExpect {
            status { isOk() }
            jsonPath("$.icon.type") { value("EXTERNAL") }
            jsonPath("$.icon.value") { value("https://x/i.png") }
        }
    }

    @Test
    fun `GET documents detail — icon 없으면 null`() {
        val doc = documentRepository.save(
            Document(
                projectId = 1L,
                notionPageId = "page-noicon-${System.nanoTime()}",
                title = "Doc Y",
            ),
        )

        mockMvc.get("/documents/${doc.id}").andExpect {
            status { isOk() }
            jsonPath("$.icon") { value(null) }
        }
    }
}
