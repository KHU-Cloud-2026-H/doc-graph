package com.docgraph.backend.document.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "block")
class Block(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(name = "notion_block_id", nullable = false, length = 100)
    val notionBlockId: String,

    @Column(name = "parent_type", length = 50)
    var parentType: String? = null,

    @Column(name = "parent_id", length = 100)
    var parentId: String? = null,

    @Column(nullable = false, length = 60)
    var type: String,

    @Column(columnDefinition = "text")
    var text: String? = null,

    @Column(name = "previous_text", columnDefinition = "text")
    var previousText: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "created_time")
    var createdTime: OffsetDateTime? = null,

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    @Column(name = "last_edited_time")
    var lastEditedTime: OffsetDateTime? = null,

    @Column(name = "last_edited_by", length = 100)
    var lastEditedBy: String? = null,

    @Column(name = "has_children", nullable = false)
    var hasChildren: Boolean = false,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "in_trash", nullable = false)
    var inTrash: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_block", columnDefinition = "jsonb")
    var rawBlock: String? = null,
) {

    /**
     * 승인된 수정 제안이 Notion에 반영된 뒤, 로컬 스냅샷을 동일 텍스트로 맞춘다.
     * 봇 쓰기는 재검증 대상이 아니므로 previousText(미검증 변경 마커)를 남기지 않는다.
     */
    fun applyApprovedText(newText: String) {
        this.text = newText
        this.previousText = null
    }

    fun refreshSnapshot(
        parentType: String?,
        parentId: String?,
        type: String,
        text: String?,
        sortOrder: Int,
        createdTime: OffsetDateTime?,
        createdBy: String?,
        lastEditedTime: OffsetDateTime?,
        lastEditedBy: String?,
        hasChildren: Boolean,
        archived: Boolean,
        inTrash: Boolean,
        rawBlock: String?,
    ) {
        previousText = this.text
        this.parentType = parentType
        this.parentId = parentId
        this.type = type
        this.text = text
        this.sortOrder = sortOrder
        this.createdTime = createdTime
        this.createdBy = createdBy
        this.lastEditedTime = lastEditedTime
        this.lastEditedBy = lastEditedBy
        this.hasChildren = hasChildren
        this.archived = archived
        this.inTrash = inTrash
        this.rawBlock = rawBlock
    }
}
