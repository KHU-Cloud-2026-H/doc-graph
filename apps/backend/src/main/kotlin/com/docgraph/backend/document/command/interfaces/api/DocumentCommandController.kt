package com.docgraph.backend.document.command.interfaces.api

import com.docgraph.backend.document.command.domain.DocumentChangeKind
import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.document.command.infra.notion.NotionWebhookProperties
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.OffsetDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Document")
class DocumentCommandController(
    private val webhookProperties: NotionWebhookProperties,
    private val noticeRepository: DocumentChangeNoticeRepository,
    @Qualifier("notionObjectMapper")
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/notion")
    @Operation(summary = "Notion Webhook 수신 (page.content_updated / page.moved)")
    fun receiveWebhook(
        @RequestHeader("X-Notion-Signature", required = false) signature: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val rawBody = request.inputStream.readBytes()

        if (!verifyHmac(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val json = objectMapper.readTree(rawBody)
        val verificationToken = json.get("verification_token")?.asText()
        if (verificationToken != null) {
            log.info("Notion webhook verification_token: {}", verificationToken)
            return ResponseEntity.ok().build()
        }

        val payload = objectMapper.treeToValue(json, NotionWebhookPayload::class.java)

        // bot이 트리거한 변경은 무시 (자동화 루프 방지)
        if (payload.authors.isNotEmpty() && payload.authors.all { it.type == "bot" }) {
            return ResponseEntity.ok().build()
        }

        val changeKind = when (payload.type) {
            "page.content_updated" -> DocumentChangeKind.CONTENT_UPDATED
            "page.moved" -> DocumentChangeKind.PARENT_MOVED
            else -> return ResponseEntity.ok().build()
        }

        // 멱등성: 동일 이벤트 재전달 무시
        if (noticeRepository.findByNotionEventId(payload.id) != null) {
            return ResponseEntity.ok().build()
        }

        noticeRepository.save(
            DocumentChangeNotice(
                changeKind = changeKind,
                notionEventId = payload.id,
                eventType = payload.type,
                notionWorkspaceId = payload.workspaceId,
                subscriptionId = payload.subscriptionId,
                notionPageId = payload.entity.id,
                entityType = payload.entity.type,
                actorType = payload.authors.firstOrNull()?.type,
                attemptNumber = payload.attemptNumber,
                occurredAt = OffsetDateTime.parse(payload.timestamp),
                rawPayload = String(rawBody, Charsets.UTF_8),
            ),
        )

        return ResponseEntity.ok().build()
    }

    private fun verifyHmac(body: ByteArray, signature: String?): Boolean {
        val secret = webhookProperties.secret
        if (secret.isBlank()) return true
        if (signature == null) return false
        val expected = "sha256=" + hmacSha256Hex(secret, body)
        return MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
    }

    private fun hmacSha256Hex(secret: String, data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionWebhookPayload(
    @Schema(description = "웹훅 이벤트 고유 ID") val id: String,
    @Schema(description = "이벤트 발생 시각 (ISO 8601)", example = "2024-12-05T19:49:36.997Z") val timestamp: String,
    @Schema(description = "이벤트 타입 (page.content_updated / page.moved)", example = "page.content_updated") val type: String,
    @JsonProperty("workspace_id") @Schema(description = "워크스페이스 ID") val workspaceId: String? = null,
    @JsonProperty("subscription_id") @Schema(description = "웹훅 구독 ID") val subscriptionId: String? = null,
    @Schema(description = "이벤트를 트리거한 엔티티") val entity: NotionEntity,
    @Schema(description = "이벤트 작성자 목록") val authors: List<NotionActor> = emptyList(),
    @JsonProperty("attempt_number") @Schema(description = "전달 시도 횟수 (최대 8회)", example = "1") val attemptNumber: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionEntity(
    @Schema(description = "엔티티 ID (페이지 ID)", example = "abc1234567890def") val id: String,
    @Schema(description = "엔티티 타입", example = "page") val type: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionActor(
    @Schema(description = "actor ID", example = "c7c11cca-1d73-471d-9b6e-bdef51470190") val id: String,
    @Schema(description = "actor 타입 (person / bot)", example = "person") val type: String,
)
