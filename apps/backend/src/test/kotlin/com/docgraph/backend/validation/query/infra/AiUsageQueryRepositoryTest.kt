package com.docgraph.backend.validation.query.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import com.docgraph.backend.validation.command.infra.AiUsageRecordRepositoryImpl
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    SharedPostgresContainer::class,
    AiUsageQueryRepository::class,
    AiUsageRecordRepositoryImpl::class,
)
class AiUsageQueryRepositoryTest @Autowired constructor(
    private val queryRepository: AiUsageQueryRepository,
    private val recordRepository: AiUsageRecordRepository,
) {

    private var taskSeq = 0L
    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")

    @Test
    fun `aggregateByModel — 프로젝트의 모델별 호출 수·토큰 합계, 타 프로젝트 제외`() {
        seed(projectId = 1L, model = "gpt-4o", prompt = 100, completion = 20, total = 120)
        seed(projectId = 1L, model = "gpt-4o", prompt = 50, completion = 10, total = 60)
        seed(projectId = 1L, model = "gpt-4o-mini", prompt = 30, completion = 5, total = 35)
        seed(projectId = 2L, model = "gpt-4o", prompt = 999, completion = 999, total = 1998)

        val byModel = queryRepository.aggregateByModel(1L).associateBy { it.model }

        assertEquals(2, byModel.size)
        val main = byModel.getValue("gpt-4o")
        assertEquals(2L, main.calls)
        assertEquals(150L, main.promptTokens)
        assertEquals(30L, main.completionTokens)
        assertEquals(180L, main.totalTokens)
        val mini = byModel.getValue("gpt-4o-mini")
        assertEquals(1L, mini.calls)
        assertEquals(30L, mini.promptTokens)
        assertEquals(35L, mini.totalTokens)
    }

    @Test
    fun `findRecordsByProject — 프로젝트 레코드만 페이지네이션 반환`() {
        seed(projectId = 1L, model = "m", prompt = 1, completion = 1, total = 2)
        seed(projectId = 1L, model = "m", prompt = 1, completion = 1, total = 2)
        seed(projectId = 1L, model = "m", prompt = 1, completion = 1, total = 2)
        seed(projectId = 2L, model = "m", prompt = 1, completion = 1, total = 2)

        val page = queryRepository.findRecordsByProject(1L, PageRequest.of(0, 2))

        assertEquals(3L, page.totalElements)
        assertEquals(2, page.content.size)
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
                createdAt = now,
            ),
        )
    }
}
