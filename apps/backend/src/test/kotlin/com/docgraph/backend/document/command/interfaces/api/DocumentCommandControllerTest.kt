package com.docgraph.backend.document.command.interfaces.api

import com.docgraph.backend.document.command.application.ProcessDocumentChangeNoticeCommandHandler
import com.docgraph.backend.document.command.domain.DocumentChangeKind
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.ninjasquad.springmockk.MockkBean
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val WEBHOOK_PATH = "/webhooks/notion"

private fun webhookPayload(
    id: String = "evt-test-1",
    type: String = "page.content_updated",
    authors: String = """[{"id":"user-1","type":"person"}]""",
) = """
    {
      "id": "$id",
      "timestamp": "2026-05-08T00:00:00Z",
      "type": "$type",
      "workspaceId": "ws-test",
      "subscriptionId": "sub-test",
      "entity": {"id": "page-abc", "type": "page"},
      "authors": $authors,
      "attemptNumber": 1
    }
""".trimIndent()

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedPostgresContainer::class)
class DocumentCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val noticeRepository: DocumentChangeNoticeRepository,
    private val em: EntityManager,
    private val txTemplate: TransactionTemplate,
) {
    // 실제 Notion API·스케줄러 처리 봉인 — 컨트롤러 계층만 검증
    @MockkBean(relaxed = true)
    lateinit var processHandler: ProcessDocumentChangeNoticeCommandHandler

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM DocumentChangeNotice").executeUpdate()
        }
    }

    @Test
    fun `유효한 webhook payload — 200 OK, PENDING notice 저장`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload()
        }.andExpect { status { isOk() } }

        val notice = noticeRepository.findByNotionEventId("evt-test-1")
        assertNotNull(notice)
        assertEquals(OutboxStatus.PENDING, notice.status)
        assertEquals(DocumentChangeKind.CONTENT_UPDATED, notice.changeKind)
        assertEquals("page-abc", notice.notionPageId)
        assertEquals("ws-test", notice.notionWorkspaceId)
    }

    @Test
    fun `page_moved 타입 — PARENT_MOVED notice 저장`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload(id = "evt-moved-1", type = "page.moved")
        }.andExpect { status { isOk() } }

        val notice = noticeRepository.findByNotionEventId("evt-moved-1")
        assertNotNull(notice)
        assertEquals(DocumentChangeKind.PARENT_MOVED, notice.changeKind)
    }

    @Test
    fun `모든 author가 bot — 200 OK, notice 저장 안 함`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload(
                id = "evt-bot-1",
                authors = """[{"id":"bot-1","type":"bot"},{"id":"bot-2","type":"bot"}]""",
            )
        }.andExpect { status { isOk() } }

        assertNull(noticeRepository.findByNotionEventId("evt-bot-1"))
    }

    @Test
    fun `동일 notionEventId 재전송 — 200 OK, 중복 저장 없음 (멱등성)`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload(id = "evt-dup-1")
        }.andExpect { status { isOk() } }

        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload(id = "evt-dup-1")
        }.andExpect { status { isOk() } }

        assertEquals(1, noticeRepository.findAll().size)
    }

    @Test
    fun `알 수 없는 이벤트 타입 — 200 OK, notice 저장 안 함`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload(id = "evt-unknown-1", type = "page.unknown")
        }.andExpect { status { isOk() } }

        assertNull(noticeRepository.findByNotionEventId("evt-unknown-1"))
    }
}

@Tag("component")
@SpringBootTest(properties = ["notion.webhook.secret=hmac-test-secret"])
@AutoConfigureMockMvc
@Import(SharedPostgresContainer::class)
class DocumentCommandControllerHmacTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockkBean(relaxed = true)
    lateinit var processHandler: ProcessDocumentChangeNoticeCommandHandler

    @Test
    fun `서명 헤더 없음 — 401`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = webhookPayload(id = "evt-hmac-1")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `잘못된 서명 — 401`() {
        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            header("X-Notion-Signature", "sha256=badhash")
            content = webhookPayload(id = "evt-hmac-2")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `올바른 HMAC 서명 — 200 OK`() {
        val body = webhookPayload(id = "evt-hmac-3")
        val sig = "sha256=" + hmacSha256Hex("hmac-test-secret", body.toByteArray())

        mockMvc.post(WEBHOOK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            header("X-Notion-Signature", sig)
            content = body
        }.andExpect { status { isOk() } }
    }

    private fun hmacSha256Hex(secret: String, data: ByteArray): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }
}
