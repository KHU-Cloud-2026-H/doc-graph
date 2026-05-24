package com.docgraph.backend.acceptance

import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.query.application.DocumentType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * acceptance profile 한정 — UC4·UC5·UC6·UC8 spec이 document 상태를 직접 seed하는 우회 진입점.
 *
 * 운영 흐름은 webhook handler + 비동기 worker가 Notion API 동기화로 Document/Block을 영속.
 * fixture는 EntityManager 직접 persist — document 도메인 JPA Repository 구현체 미존재라
 * Repository interface DI 불가.
 *
 * 시드 요청은 entity invariants를 그대로 반영(필수 필드 require, 선택 필드 nullable).
 * 식별자(notionPageId, notionBlockId)만 미지정 시 자동 생성으로 ergonomics 보강.
 */
@RestController
@Profile("acceptance")
class DocumentFixtureController(
    @PersistenceContext private val em: EntityManager,
) {

    @PostMapping("/test/documents")
    @Transactional
    fun seedDocument(@RequestBody request: SeedDocumentRequest): ResponseEntity<SeedDocumentResponse> {
        val document = Document(
            projectId = request.projectId,
            notionPageId = request.notionPageId ?: "page-fixture-${System.nanoTime()}",
            parentNotionPageId = request.parentNotionPageId,
            parentDocumentId = request.parentDocumentId,
            title = request.title,
            type = request.type,
            assigneeMemberId = request.assigneeMemberId,
        )
        em.persist(document)

        val blocks = request.blocks.map { item ->
            val block = Block(
                document = document,
                notionBlockId = item.notionBlockId ?: "block-fixture-${System.nanoTime()}",
                parentType = item.parentType,
                parentId = item.parentId,
                type = item.type,
                text = item.text,
                sortOrder = item.sortOrder,
            )
            em.persist(block)
            block
        }

        return ResponseEntity.ok(
            SeedDocumentResponse(
                id = document.id,
                blocks = blocks.map { BlockIdResponse(it.id, it.notionBlockId) },
            ),
        )
    }
}

data class SeedDocumentRequest(
    @Schema(example = "1")
    val projectId: Long,
    @Schema(example = "2024-01 스프린트 회의록")
    val title: String,
    @Schema(description = "지정 시 명시 페이지 ID 사용, 없으면 fixture가 자동 생성", example = "abc1234567890def")
    val notionPageId: String? = null,
    @Schema(description = "Notion 부모 페이지 ID", example = "parent1234567890")
    val parentNotionPageId: String? = null,
    @Schema(description = "부모 Document row id (같은 프로젝트 내)", example = "1")
    val parentDocumentId: Long? = null,
    @Schema(description = "문서 타입 (미분류면 null)")
    val type: DocumentType? = null,
    @Schema(description = "담당자 workspace_member row id", example = "1")
    val assigneeMemberId: Long? = null,
    @Schema(description = "블록 목록 (옵션)")
    val blocks: List<SeedBlockItem> = emptyList(),
)

data class SeedBlockItem(
    @Schema(description = "Notion 블록 타입 (e.g. paragraph)", example = "paragraph")
    val type: String,
    @Schema(example = "0")
    val sortOrder: Int,
    @Schema(description = "지정 시 명시 block ID 사용, 없으면 fixture가 자동 생성", example = "block-abc")
    val notionBlockId: String? = null,
    @Schema(description = "부모 컨테이너 타입 (e.g. page_id, block_id)", example = "page_id")
    val parentType: String? = null,
    @Schema(description = "부모 컨테이너 ID", example = "abc1234567890def")
    val parentId: String? = null,
    @Schema(description = "블록 텍스트", example = "회의 결정 사항")
    val text: String? = null,
)

data class SeedDocumentResponse(
    val id: Long,
    val blocks: List<BlockIdResponse>,
)

data class BlockIdResponse(
    val id: Long,
    val notionBlockId: String,
)
