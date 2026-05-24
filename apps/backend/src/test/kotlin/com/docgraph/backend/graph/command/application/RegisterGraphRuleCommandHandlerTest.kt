package com.docgraph.backend.graph.command.application

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleDuplicateException
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Tag("unit")
class RegisterGraphRuleCommandHandlerTest {

    private val ruleRepository = mockk<GraphRuleRepository>()
    private val handler = RegisterGraphRuleCommandHandler(ruleRepository)

    @Test
    fun `handle — 룰 저장 후 id 반환`() {
        val capturedRule = slot<GraphRule>()
        every {
            ruleRepository.findAllByProjectIdAndTypePair(1L, DocumentType.PLANNING, DocumentType.REQUIREMENTS)
        } returns emptyList()
        every { ruleRepository.save(capture(capturedRule)) } answers {
            capturedRule.captured.apply { id = 300L }
        }

        val ruleId = handler.handle(
            RegisterGraphRuleCommand(
                projectId = 1L,
                sourceType = DocumentType.PLANNING,
                targetType = DocumentType.REQUIREMENTS,
                validationCriterion = "범위 일치 여부",
            ),
        )

        assertEquals(300L, ruleId)
        assertEquals(1L, capturedRule.captured.projectId)
        assertEquals(false, capturedRule.captured.isDefault)
    }

    @Test
    fun `handle — 동일 (source, target) 룰이 있으면 GraphRuleDuplicateException`() {
        val existing = GraphRule(
            id = 299L,
            projectId = 1L,
            sourceType = DocumentType.PLANNING,
            targetType = DocumentType.REQUIREMENTS,
            validationCriterion = "기존 기준",
            isDefault = false,
        )
        every {
            ruleRepository.findAllByProjectIdAndTypePair(1L, DocumentType.PLANNING, DocumentType.REQUIREMENTS)
        } returns listOf(existing)

        assertThrows<GraphRuleDuplicateException> {
            handler.handle(
                RegisterGraphRuleCommand(
                    projectId = 1L,
                    sourceType = DocumentType.PLANNING,
                    targetType = DocumentType.REQUIREMENTS,
                    validationCriterion = "범위 일치 여부",
                ),
            )
        }
    }

    @Test
    fun `handle — sourceType과 targetType이 같으면 IllegalArgumentException (entity require)`() {
        every {
            ruleRepository.findAllByProjectIdAndTypePair(1L, DocumentType.PLANNING, DocumentType.PLANNING)
        } returns emptyList()

        assertThrows<IllegalArgumentException> {
            handler.handle(
                RegisterGraphRuleCommand(
                    projectId = 1L,
                    sourceType = DocumentType.PLANNING,
                    targetType = DocumentType.PLANNING,
                    validationCriterion = "범위 일치 여부",
                ),
            )
        }
    }
}
