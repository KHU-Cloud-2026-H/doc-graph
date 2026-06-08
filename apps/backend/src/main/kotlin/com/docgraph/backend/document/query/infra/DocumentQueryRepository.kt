package com.docgraph.backend.document.query.infra

import com.docgraph.backend.document.command.domain.QBlock
import com.docgraph.backend.document.command.domain.QDocument
import com.docgraph.backend.document.query.application.Block
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentNodeData
import com.docgraph.backend.document.query.application.DocumentReference
import com.docgraph.backend.document.query.application.DocumentStats
import com.docgraph.backend.document.query.application.DocumentSummary
import com.docgraph.backend.document.query.application.IconResponse
import com.docgraph.backend.document.query.application.IconType
import com.docgraph.backend.document.query.application.NotionPageRef
import com.docgraph.backend.project.query.application.AssignedDocumentType
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class DocumentQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)


    fun findDetail(documentId: Long): DocumentDetail? {
        val doc = QDocument.document
        val blk = QBlock.block

        val document = queryFactory
            .selectFrom(doc)
            .where(doc.id.eq(documentId))
            .fetchOne()
            ?: return null

        val blocks = queryFactory
            .selectFrom(blk)
            .where(blk.document.id.eq(documentId))
            .orderBy(blk.sortOrder.asc())
            .fetch()
            .map { b ->
                Block(
                    blockId = b.notionBlockId,
                    parentBlockId = if (b.parentType == "block_id") b.parentId else null,
                    type = b.type,
                    text = b.text,
                    previousText = b.previousText,
                    order = b.sortOrder,
                )
            }

        return DocumentDetail(
            id = document.id,
            notionPageId = document.notionPageId,
            title = document.title,
            type = document.type,
            parentDocumentId = document.parentDocumentId,
            icon = toIcon(document.iconType, document.iconValue),
            assigneeMemberId = document.assigneeMemberId,
            notionLastEditedAt = document.notionLastEditedAt,
            lastEditedByName = null,
            flatText = document.flatText,
            blocks = blocks,
        )
    }

    /** 최근 수정자 Notion user id. 이름 해석은 auth Query API를 호출하는 application 핸들러가 담당한다. */
    fun findNotionLastEditedById(documentId: Long): String? {
        val doc = QDocument.document
        return queryFactory
            .select(doc.notionLastEditedBy)
            .from(doc)
            .where(doc.id.eq(documentId))
            .fetchFirst()
    }

    fun findPageInfoByNotionPageIds(notionPageIds: Collection<String>): List<NotionPageRef> {
        if (notionPageIds.isEmpty()) return emptyList()
        val doc = QDocument.document
        return queryFactory
            .selectFrom(doc)
            .where(doc.notionPageId.`in`(notionPageIds))
            .fetch()
            .map { d ->
                NotionPageRef(
                    notionPageId = d.notionPageId,
                    title = d.title,
                    icon = toIcon(d.iconType, d.iconValue),
                )
            }
    }

    private fun toIcon(iconType: IconType?, iconValue: String?): IconResponse? =
        iconType?.let { IconResponse(type = it, value = iconValue ?: "") }

    fun searchSummariesByProject(projectId: Long, pageable: Pageable): Page<DocumentSummary> {
        val doc = QDocument.document

        val count = queryFactory
            .select(doc.count())
            .from(doc)
            .where(doc.projectId.eq(projectId))
            .fetchOne() ?: 0L

        val content = queryFactory
            .selectFrom(doc)
            .where(doc.projectId.eq(projectId))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { d ->
                DocumentSummary(
                    id = d.id,
                    notionPageId = d.notionPageId,
                    title = d.title,
                    type = d.type,
                    parentDocumentId = d.parentDocumentId,
                    icon = toIcon(d.iconType, d.iconValue),
                )
            }

        return PageImpl(content, pageable, count)
    }

    fun searchNodesByProject(projectId: Long): List<DocumentNodeData> {
        val doc = QDocument.document
        return queryFactory
            .selectFrom(doc)
            .where(doc.projectId.eq(projectId))
            .fetch()
            .map { d ->
                DocumentNodeData(
                    id = d.id,
                    title = d.title,
                    type = d.type,
                    assigneeMemberId = d.assigneeMemberId,
                )
            }
    }

    fun searchIdsByAssignee(memberId: Long): List<Long> {
        val doc = QDocument.document
        return queryFactory
            .select(doc.id)
            .from(doc)
            .where(doc.assigneeMemberId.eq(memberId))
            .fetch()
    }

    fun findReferencesByIds(documentIds: List<Long>): List<DocumentReference> {
        if (documentIds.isEmpty()) return emptyList()
        val doc = QDocument.document
        return queryFactory
            .select(
                Projections.constructor(
                    DocumentReference::class.java,
                    doc.id,
                    doc.projectId,
                    doc.title,
                    doc.type,
                    doc.notionLastEditedAt,
                ),
            )
            .from(doc)
            .where(doc.id.`in`(documentIds))
            .fetch()
    }

    fun searchIdsByProjectAndTypes(typeAssignments: List<AssignedDocumentType>): List<Long> {
        if (typeAssignments.isEmpty()) return emptyList()
        val doc = QDocument.document
        val predicate = BooleanBuilder()
        typeAssignments.forEach { a ->
            predicate.or(doc.projectId.eq(a.projectId).and(doc.type.eq(a.documentType)))
        }
        return queryFactory
            .select(doc.id)
            .from(doc)
            .where(predicate)
            .fetch()
    }

    fun searchUnassignedIdsByProject(projectId: Long): List<Long> {
        val doc = QDocument.document
        return queryFactory
            .select(doc.id)
            .from(doc)
            .where(doc.projectId.eq(projectId).and(doc.assigneeMemberId.isNull))
            .fetch()
    }

    fun searchStatsByProjectIds(projectIds: Collection<Long>): Map<Long, DocumentStats> {
        if (projectIds.isEmpty()) return emptyMap()
        val doc = QDocument.document
        return queryFactory
            .select(doc.projectId, doc.count(), doc.notionLastEditedAt.max())
            .from(doc)
            .where(doc.projectId.`in`(projectIds))
            .groupBy(doc.projectId)
            .fetch()
            .associate { tuple ->
                val projectId = tuple.get(doc.projectId)!!
                val count = tuple.get(doc.count())!!
                val maxEditedAt = tuple.get(doc.notionLastEditedAt.max())
                projectId to DocumentStats(documentCount = count, lastNotionChangedAt = maxEditedAt)
            }
    }
}
