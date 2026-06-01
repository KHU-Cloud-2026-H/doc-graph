package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.AiUsage
import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Tag("unit")
class RecordAiUsageCommandHandlerTest {

    private val repository = mockk<AiUsageRecordRepository>()
    private val handler = RecordAiUsageCommandHandler(repository)

    @Test
    fun `신규 — command 필드로 AiUsageRecord 저장`() {
        every { repository.findByValidationTaskId(7L) } returns null
        val saved = slot<AiUsageRecord>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        handler.handle(RecordAiUsageCommand(7L, 3L, AiUsage("gpt-4o-2024-08-06", 120, 30, 150)))

        assertEquals(7L, saved.captured.validationTaskId)
        assertEquals(3L, saved.captured.projectId)
        assertEquals("gpt-4o-2024-08-06", saved.captured.model)
        assertEquals(120, saved.captured.promptTokens)
        assertEquals(30, saved.captured.completionTokens)
        assertEquals(150, saved.captured.totalTokens)
    }

    @Test
    fun `이미 기록됨 — idempotent skip`() {
        every { repository.findByValidationTaskId(7L) } returns mockk()

        handler.handle(RecordAiUsageCommand(7L, 3L, AiUsage("m", 1, 1, 2)))

        verify(exactly = 0) { repository.save(any()) }
    }
}
