package com.docgraph.backend.validation.query.interfaces.api

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedPostgresContainer::class)
class AiUsageQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val recordRepository: AiUsageRecordRepository,
    private val em: EntityManager,
    private val txTemplate: TransactionTemplate,
) {

    private var taskSeq = 0L

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM AiUsageRecord").executeUpdate()
        }
        taskSeq = 0L
    }

    @Test
    fun `GET ai-usage — 프로젝트 합계와 모델별 분해 반환`() {
        seed(projectId = 77L, model = "gpt-4o", prompt = 100, completion = 20, total = 120)
        seed(projectId = 77L, model = "gpt-4o", prompt = 50, completion = 10, total = 60)
        seed(projectId = 77L, model = "gpt-4o-mini", prompt = 30, completion = 5, total = 35)
        seed(projectId = 88L, model = "gpt-4o", prompt = 999, completion = 999, total = 1998)

        mockMvc.get("/projects/77/ai-usage").andExpect {
            status { isOk() }
            jsonPath("$.totalCalls") { value(3) }
            jsonPath("$.totalTokens") { value(215) }
            jsonPath("$.byModel.length()") { value(2) }
        }
    }

    @Test
    fun `GET ai-usage records — 프로젝트 호출 이력 페이지`() {
        seed(projectId = 77L, model = "m", prompt = 1, completion = 1, total = 2)
        seed(projectId = 77L, model = "m", prompt = 1, completion = 1, total = 2)
        seed(projectId = 77L, model = "m", prompt = 1, completion = 1, total = 2)
        seed(projectId = 88L, model = "m", prompt = 1, completion = 1, total = 2)

        mockMvc.get("/projects/77/ai-usage/records") {
            param("size", "2")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(3) }
            jsonPath("$.content.length()") { value(2) }
        }
    }

    private fun seed(projectId: Long, model: String, prompt: Int, completion: Int, total: Int) {
        recordRepository.save(
            AiUsageRecord(
                validationTaskId = ++taskSeq,
                projectId = projectId,
                model = model,
                promptTokens = prompt,
                completionTokens = completion,
                totalTokens = total,
                createdAt = OffsetDateTime.now(),
            ),
        )
    }
}
