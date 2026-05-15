package com.docgraph.backend.document.command.domain

import com.docgraph.backend.document.query.application.DocumentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "document")
class Document(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id", nullable = false)
    val projectId: Long,

    @Column(name = "notion_page_id", nullable = false, length = 100)
    val notionPageId: String,

    @Column(name = "parent_notion_page_id", length = 100)
    var parentNotionPageId: String? = null,

    @Column(nullable = false, length = 500)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    var type: DocumentType? = null,

    @Column(name = "assignee_member_id")
    var assigneeMemberId: Long? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_content", columnDefinition = "jsonb")
    var rawContent: String? = null,

    @Column(name = "flat_text", columnDefinition = "text")
    var flatText: String? = null,

    @Column(name = "notion_created_by", length = 100)
    var notionCreatedBy: String? = null,

    @Column(name = "notion_last_edited_by", length = 100)
    var notionLastEditedBy: String? = null,

    @Column(name = "notion_last_edited_at")
    var notionLastEditedAt: OffsetDateTime? = null,

    @Column(name = "synced_at", nullable = false)
    var syncedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {

    fun refreshSnapshot(
        title: String,
        parentNotionPageId: String?,
        type: DocumentType?,
        assigneeMemberId: Long?,
        rawContent: String?,
        flatText: String?,
        notionCreatedBy: String?,
        notionLastEditedBy: String?,
        notionLastEditedAt: OffsetDateTime?,
    ) {
        this.title = title
        this.parentNotionPageId = parentNotionPageId
        this.type = type
        this.assigneeMemberId = assigneeMemberId
        this.rawContent = rawContent
        this.flatText = flatText
        this.notionCreatedBy = notionCreatedBy
        this.notionLastEditedBy = notionLastEditedBy
        this.notionLastEditedAt = notionLastEditedAt
        syncedAt = OffsetDateTime.now()
        updatedAt = OffsetDateTime.now()
    }
}
