package com.docgraph.backend.document.command.infra.notion

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Notion 블록 JSON → NotionBlock 매핑(extractPlainText·toDomainBlock)의 type별 추출을 단위 검증한다.
 * HTTP 전송만 MockRestServiceServer로 격리 — 실 네트워크·Spring 컨텍스트 없음.
 */
@Tag("unit")
class NotionDocumentRestClientTest {

    private val objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .addModule(KotlinModule.Builder().build())
        .build()

    private lateinit var server: MockRestServiceServer
    private lateinit var client: NotionDocumentRestClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl(BASE_URL)
        server = MockRestServiceServer.bindTo(builder).build()
        client = NotionDocumentRestClient(builder.build(), objectMapper)
    }

    @Test
    fun `rich_text 블록은 plain_text만 합치고 annotations·href는 버린다`() {
        stubChildren(
            """
            {"id":"p1","type":"paragraph","has_children":false,
             "parent":{"type":"page_id","page_id":"page-1"},
             "paragraph":{"rich_text":[
               {"type":"text","plain_text":"굵게","annotations":{"bold":true},"href":null},
               {"type":"text","plain_text":" 그리고 링크","annotations":{"italic":true},"href":"https://example.com"}
             ]}}
            """.trimIndent(),
        )

        val block = client.fetchBlockChildren("blk").single()

        assertEquals("paragraph", block.type)
        assertEquals("굵게 그리고 링크", block.text)
        assertEquals("page_id", block.parentType)
        assertEquals("page-1", block.parentId)
    }

    @Test
    fun `child_page는 rich_text가 없어 title을 text로 추출한다`() {
        stubChildren(
            """
            {"id":"cp1","type":"child_page","has_children":true,
             "parent":{"type":"page_id","page_id":"page-1"},
             "child_page":{"title":"API 명세서"}}
            """.trimIndent(),
        )

        val block = client.fetchBlockChildren("blk").single()

        assertEquals("child_page", block.type)
        assertEquals("API 명세서", block.text)
        assertEquals("API 명세서", block.childPageTitle)
    }

    @Test
    fun `rich_text·title이 모두 없는 table·image 블록은 text가 null`() {
        stubChildren(
            """
            {"id":"tb1","type":"table","has_children":true,
             "parent":{"type":"page_id","page_id":"page-1"},
             "table":{"table_width":2,"has_column_header":true,"has_row_header":false}}
            """.trimIndent(),
            """
            {"id":"img1","type":"image","has_children":false,
             "parent":{"type":"page_id","page_id":"page-1"},
             "image":{"type":"external","external":{"url":"https://img.example/x.png"}}}
            """.trimIndent(),
        )

        val blocks = client.fetchBlockChildren("blk")

        val table = blocks.first { it.type == "table" }
        val image = blocks.first { it.type == "image" }
        assertNull(table.text)
        assertNull(image.text)
    }

    @Test
    fun `heading_4 타입과 텍스트를 그대로 보존한다`() {
        stubChildren(
            """
            {"id":"h4","type":"heading_4","has_children":false,
             "parent":{"type":"page_id","page_id":"page-1"},
             "heading_4":{"rich_text":[{"type":"text","plain_text":"비고","annotations":{}}]}}
            """.trimIndent(),
        )

        val block = client.fetchBlockChildren("blk").single()

        assertEquals("heading_4", block.type)
        assertEquals("비고", block.text)
    }

    @Test
    fun `parent가 block_id면 parentType·parentId로 중첩을 보존한다`() {
        stubChildren(
            """
            {"id":"child","type":"bulleted_list_item","has_children":false,
             "parent":{"type":"block_id","block_id":"parent-block"},
             "bulleted_list_item":{"rich_text":[{"type":"text","plain_text":"하위 항목"}]}}
            """.trimIndent(),
        )

        val block = client.fetchBlockChildren("blk").single()

        assertEquals("block_id", block.parentType)
        assertEquals("parent-block", block.parentId)
        assertEquals("하위 항목", block.text)
    }

    @Test
    fun `page mention은 linkedPageIds로 추출한다`() {
        stubChildren(
            """
            {"id":"m1","type":"paragraph","has_children":false,
             "parent":{"type":"page_id","page_id":"page-1"},
             "paragraph":{"rich_text":[
               {"type":"mention","mention":{"type":"page","page":{"id":"linked-page-1"}},"plain_text":"링크된 페이지"}
             ]}}
            """.trimIndent(),
        )

        val block = client.fetchBlockChildren("blk").single()

        assertEquals("링크된 페이지", block.text)
        assertTrue("linked-page-1" in block.linkedPageIds)
    }

    private fun stubChildren(vararg resultJson: String) {
        val body = """{"results":[${resultJson.joinToString(",")}],"next_cursor":null}"""
        server.expect(requestTo("$BASE_URL/v1/blocks/blk/children"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))
    }

    private companion object {
        const val BASE_URL = "https://api.notion.test"
    }
}