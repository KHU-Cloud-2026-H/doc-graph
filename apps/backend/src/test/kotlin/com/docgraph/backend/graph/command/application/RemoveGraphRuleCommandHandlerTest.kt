package com.docgraph.backend.graph.command.application

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.graph.command.domain.DefaultGraphRuleCannotBeRemovedException
import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleNotFoundException
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Tag("unit")
class RemoveGraphRuleCommandHandlerTest {

    private val ruleRepository = mockk<GraphRuleRepository>(relaxUnitFun = true)
    private val handler = RemoveGraphRuleCommandHandler(ruleRepository)

    @Test
    fun `handle — 커스텀 룰 삭제`() {
        val rule = GraphRule(
            id = 300L,
            projectId = 1L,
            sourceType = DocumentType.PLANNING,
            targetType = DocumentType.REQUIREMENTS,
            validationCriterion = "범위 일치 여부",
            isDefault = false,
        )
        every { ruleRepository.findByProjectIdAndId(1L, 300L) } returns rule

        handler.handle(RemoveGraphRuleCommand(projectId = 1L, ruleId = 300L))

        verify { ruleRepository.delete(rule) }
    }

    @Test
    fun `handle — 룰 없으면 GraphRuleNotFoundException`() {
        every { ruleRepository.findByProjectIdAndId(1L, 300L) } returns null

        assertThrows<GraphRuleNotFoundException> {
            handler.handle(RemoveGraphRuleCommand(projectId = 1L, ruleId = 300L))
        }
    }

    @Test
    fun `handle — 기본 제공 룰은 DefaultGraphRuleCannotBeRemovedException`() {
        val defaultRule = GraphRule(
            id = 300L,
            projectId = null,
            sourceType = DocumentType.PLANNING,
            targetType = DocumentType.REQUIREMENTS,
            validationCriterion = "범위 일치 여부",
            isDefault = true,
        )
        every { ruleRepository.findByProjectIdAndId(1L, 300L) } returns defaultRule

        assertThrows<DefaultGraphRuleCannotBeRemovedException> {
            handler.handle(RemoveGraphRuleCommand(projectId = 1L, ruleId = 300L))
        }
    }
}
