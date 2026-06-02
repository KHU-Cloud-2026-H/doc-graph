package com.docgraph.backend.document.command.infra.notion

import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionIcon
import com.docgraph.backend.document.command.domain.NotionIconType
import com.docgraph.backend.document.command.domain.NotionPage
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionSearchPage
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import com.docgraph.backend.config.ConditionalOnRealHttpAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

@Configuration
@EnableConfigurationProperties(NotionDocumentProperties::class)
class NotionDocumentClientConfig {
    @Bean
    fun notionObjectMapper(): ObjectMapper = JsonMapper.builder()
        .findAndAddModules()
        .addModule(KotlinModule.Builder().build())
        .build()

    @Bean
    @ConditionalOnRealHttpAdapter
    fun notionRestClient(properties: NotionDocumentProperties): RestClient {
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("Notion-Version", "2022-06-28")
            .build()
    }
}

@Component
@ConditionalOnRealHttpAdapter
class NotionDocumentRestClient(
    @Qualifier("notionRestClient")
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) : NotionDocumentClient {
    override fun fetchPage(pageId: String, accessToken: String?): NotionPage {
        val node = restClient.get()
            .uri("/v1/pages/{pageId}", pageId)
            .headers { headers ->
                if (!accessToken.isNullOrBlank()) {
                    headers.setBearerAuth(accessToken)
                }
            }
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: error("empty Notion page response: $pageId")

        return NotionPage(
            id = node.path("id").asText(pageId),
            title = extractTitle(node).ifBlank { "Untitled" },
            icon = extractIcon(node),
            createdTime = node.path("created_time").asOffsetDateTimeOrNull(),
            lastEditedTime = node.path("last_edited_time").asOffsetDateTimeOrNull(),
            createdBy = node.path("created_by").path("id").asTextOrNull(),
            lastEditedBy = node.path("last_edited_by").path("id").asTextOrNull(),
            rawJson = objectMapper.writeValueAsString(node),
        )
    }

    override fun fetchBlockChildren(blockId: String, accessToken: String?): List<NotionBlock> {
        val result = mutableListOf<NotionBlock>()
        var cursor: String? = null
        do {
            val startCursor = cursor
            val node = restClient.get()
                .uri { builder ->
                    val path = builder.path("/v1/blocks/{blockId}/children")
                    if (startCursor != null) {
                        path.queryParam("start_cursor", startCursor)
                    }
                    path.build(blockId)
                }
                .headers { headers ->
                    if (!accessToken.isNullOrBlank()) {
                        headers.setBearerAuth(accessToken)
                    }
                }
                .retrieve()
                .body(String::class.java)
                ?.let(objectMapper::readTree)
                ?: error("empty Notion block children response: $blockId")

            node.path("results").forEach { result += it.toDomainBlock() }
            cursor = node.path("next_cursor").asTextOrNull()
        } while (cursor != null)
        return result
    }

    override fun searchPages(accessToken: String, query: String?, pageSize: Int): List<NotionSearchPage> {
        val result = mutableListOf<NotionSearchPage>()
        var cursor: String? = null
        do {
            val body = mutableMapOf<String, Any>(
                "filter" to mapOf(
                    "value" to "page",
                    "property" to "object",
                ),
                "page_size" to pageSize.coerceIn(1, 100),
            )
            if (!query.isNullOrBlank()) {
                body["query"] = query
            }
            if (cursor != null) {
                body["start_cursor"] = cursor
            }

            val node = restClient.post()
                .uri("/v1/search")
                .headers { it.setBearerAuth(accessToken) }
                .body(body)
                .retrieve()
                .body(String::class.java)
                ?.let(objectMapper::readTree)
                ?: error("empty Notion search response")

            node.path("results")
                .filter { it.path("object").asTextOrNull() == "page" }
                .mapTo(result) { page ->
                    NotionSearchPage(
                        id = page.path("id").asText(),
                        title = extractTitle(page).ifBlank { "Untitled" },
                        icon = extractIcon(page),
                        parentType = page.path("parent").path("type").asTextOrNull(),
                        parentId = extractParentId(page.path("parent")),
                        url = page.path("url").asTextOrNull(),
                        lastEditedTime = page.path("last_edited_time").asOffsetDateTimeOrNull(),
                    )
                }
            cursor = node.path("next_cursor").asTextOrNull()
        } while (cursor != null)
        return result
    }

    override fun patchBlockText(
        notionBlockId: String,
        newText: String,
        expectedLastEditedAt: OffsetDateTime?,
        accessToken: String?,
    ): NotionPatchResult {
        val block = fetchBlock(notionBlockId, accessToken)
        val type = block.path("type").asText("unsupported")
        require(type in richTextBlockTypes) { "unsupported Notion text patch block type: $type" }

        val lastEditedTime = block.path("last_edited_time").asOffsetDateTimeOrNull()
        if (expectedLastEditedAt != null && lastEditedTime != null && lastEditedTime.isAfter(expectedLastEditedAt)) {
            error("Notion block is newer than expected: $notionBlockId")
        }

        val body = mapOf(
            type to mapOf(
                "rich_text" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to mapOf("content" to newText),
                    ),
                ),
            ),
        )

        restClient.patch()
            .uri("/v1/blocks/{blockId}", notionBlockId)
            .headers { headers ->
                if (!accessToken.isNullOrBlank()) {
                    headers.setBearerAuth(accessToken)
                }
            }
            .body(body)
            .retrieve()
            .toBodilessEntity()

        return NotionPatchResult.Success
    }

    private fun JsonNode.toDomainBlock(): NotionBlock {
        val type = path("type").asText("unsupported")
        val typed = path(type)
        return NotionBlock(
            id = path("id").asText(),
            type = type,
            parentType = path("parent").path("type").asTextOrNull(),
            parentId = extractParentId(path("parent")),
            text = extractPlainText(typed),
            linkedPageIds = extractLinkedPageIds(this),
            childPageTitle = typed.path("title").asTextOrNull(),
            createdTime = path("created_time").asOffsetDateTimeOrNull(),
            lastEditedTime = path("last_edited_time").asOffsetDateTimeOrNull(),
            createdBy = path("created_by").path("id").asTextOrNull(),
            lastEditedBy = path("last_edited_by").path("id").asTextOrNull(),
            hasChildren = path("has_children").asBoolean(false),
            archived = path("archived").asBoolean(false),
            inTrash = path("in_trash").asBoolean(false),
            rawJson = objectMapper.writeValueAsString(this),
        )
    }

    private fun fetchBlock(blockId: String, accessToken: String?): JsonNode =
        restClient.get()
            .uri("/v1/blocks/{blockId}", blockId)
            .headers { headers ->
                if (!accessToken.isNullOrBlank()) {
                    headers.setBearerAuth(accessToken)
                }
            }
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: error("empty Notion block response: $blockId")

    private fun extractTitle(page: JsonNode): String {
        val properties = page.path("properties")
        for ((_, property) in properties.properties()) {
            val title = property.path("title")
            if (title.isArray) {
                return title.joinToString("") { it.path("plain_text").asText("") }
            }
        }
        return ""
    }

    private fun extractIcon(page: JsonNode): NotionIcon? {
        val icon = page.path("icon")
        return when (icon.path("type").asTextOrNull()) {
            "emoji" -> icon.path("emoji").asTextOrNull()
                ?.let { NotionIcon(NotionIconType.EMOJI, it) }
            "external" -> icon.path("external").path("url").asTextOrNull()
                ?.let { NotionIcon(NotionIconType.EXTERNAL, it) }
            "file" -> icon.path("file").path("url").asTextOrNull()
                ?.let { NotionIcon(NotionIconType.FILE, it) }
            else -> null
        }
    }

    private fun extractPlainText(typed: JsonNode): String? {
        val richText = typed.path("rich_text")
        if (richText.isArray) {
            return richText.joinToString("") { it.path("plain_text").asText("") }
                .ifBlank { null }
        }
        return typed.path("title").asTextOrNull()
    }

    private fun extractLinkedPageIds(node: JsonNode): Set<String> {
        val ids = linkedSetOf<String>()
        fun visit(current: JsonNode) {
            if (current.isObject) {
                val mention = current.path("mention")
                val pageId = mention.path("page").path("id").asTextOrNull()
                if (mention.path("type").asTextOrNull() == "page" && pageId != null) {
                    ids += pageId
                }
                current.properties().forEach { (_, child) -> visit(child) }
            } else if (current.isArray) {
                current.forEach(::visit)
            }
        }
        visit(node)
        return ids
    }

    private fun extractParentId(parent: JsonNode): String? {
        val type = parent.path("type").asTextOrNull() ?: return null
        return parent.path(type).asTextOrNull()
    }
}

private val richTextBlockTypes = setOf(
    "paragraph",
    "heading_1",
    "heading_2",
    "heading_3",
    "bulleted_list_item",
    "numbered_list_item",
    "to_do",
    "toggle",
    "quote",
    "callout",
)

private fun JsonNode.asTextOrNull(): String? =
    takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }

private fun JsonNode.asOffsetDateTimeOrNull(): OffsetDateTime? =
    asTextOrNull()?.let { OffsetDateTime.parse(it) }
