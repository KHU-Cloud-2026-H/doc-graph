package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, AiUsageRecordRepositoryImpl::class)
class AiUsageRecordRepositoryImplTest @Autowired constructor(
    private val repository: AiUsageRecordRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `save·findByValidationTaskId — 저장한 사용량을 모든 필드로 반환`() {
        val saved = repository.save(
            record(validationTaskId = 10L, projectId = 5L, model = "gpt-4o-2024-08-06", prompt = 120, completion = 30, total = 150),
        )
        em.flush()
        em.clear()

        val found = repository.findByValidationTaskId(10L)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertEquals(5L, found.projectId)
        assertEquals("gpt-4o-2024-08-06", found.model)
        assertEquals(120, found.promptTokens)
        assertEquals(30, found.completionTokens)
        assertEquals(150, found.totalTokens)
    }

    @Test
    fun `findByValidationTaskId — 없으면 null`() {
        assertEquals(null, repository.findByValidationTaskId(999L))
    }

    @Test
    fun `validation_task_id UNIQUE — 같은 task로 두 번 저장하면 제약 위반`() {
        repository.save(record(validationTaskId = 20L, projectId = 1L))
        em.flush()

        // IDENTITY 생성이라 INSERT가 save() 시점에 실행되고 Spring이 제약 위반을 번역한다.
        assertThrows(DataIntegrityViolationException::class.java) {
            repository.save(record(validationTaskId = 20L, projectId = 1L))
        }
    }

    private fun record(
        validationTaskId: Long,
        projectId: Long,
        model: String = "m",
        prompt: Int = 1,
        completion: Int = 1,
        total: Int = 2,
    ) = AiUsageRecord(
        validationTaskId = validationTaskId,
        projectId = projectId,
        model = model,
        promptTokens = prompt,
        completionTokens = completion,
        totalTokens = total,
    )
}
