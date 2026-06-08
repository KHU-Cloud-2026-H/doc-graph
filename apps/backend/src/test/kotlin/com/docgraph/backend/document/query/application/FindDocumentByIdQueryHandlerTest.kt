package com.docgraph.backend.document.query.application

import com.docgraph.backend.auth.query.application.SearchUserNamesByNotionUserIdsQuery
import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("unit")
class FindDocumentByIdQueryHandlerTest {

    private val repository = mockk<DocumentQueryRepository>()
    private val searchUserNames = mockk<SearchUserNamesByNotionUserIdsQuery>()
    private val handler = FindDocumentByIdQueryHandler(repository, searchUserNames)

    private fun detail() = DocumentDetail(
        id = 1L,
        notionPageId = "page-1",
        title = "Doc",
        type = null,
        parentDocumentId = null,
        icon = null,
        assigneeMemberId = null,
        notionLastEditedAt = null,
        lastEditedByName = null,
        flatText = null,
        blocks = emptyList(),
    )

    @Test
    fun `document 없으면 null, 이름 조회도 안 함`() {
        every { repository.findDetail(1L) } returns null

        assertNull(handler.find(1L))
    }

    @Test
    fun `수정자 id가 앱 사용자면 lastEditedByName 채움`() {
        every { repository.findDetail(1L) } returns detail()
        every { repository.findNotionLastEditedById(1L) } returns "notion-user-1"
        every { searchUserNames.search(listOf("notion-user-1")) } returns mapOf("notion-user-1" to "홍길동")

        assertEquals("홍길동", handler.find(1L)!!.lastEditedByName)
    }

    @Test
    fun `수정자 id는 있으나 앱 사용자가 아니면 lastEditedByName null`() {
        every { repository.findDetail(1L) } returns detail()
        every { repository.findNotionLastEditedById(1L) } returns "notion-bot-9"
        every { searchUserNames.search(listOf("notion-bot-9")) } returns emptyMap()

        assertNull(handler.find(1L)!!.lastEditedByName)
    }

    @Test
    fun `수정자 id 자체가 없으면 lastEditedByName null, 이름 조회 안 함`() {
        every { repository.findDetail(1L) } returns detail()
        every { repository.findNotionLastEditedById(1L) } returns null

        assertNull(handler.find(1L)!!.lastEditedByName)
    }
}
