package com.docgraph.backend.document.query.infra

import com.docgraph.backend.document.command.domain.QBlock
import com.docgraph.backend.document.command.domain.QDocument
import com.docgraph.backend.document.query.application.Block
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentSummary
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
                    order = b.sortOrder,
                )
            }

        return DocumentDetail(
            id = document.id,
            notionPageId = document.notionPageId,
            title = document.title,
            type = document.type,
            parentDocumentId = document.parentDocumentId,
            assigneeMemberId = document.assigneeMemberId,
            notionLastEditedAt = document.notionLastEditedAt,
            blocks = blocks,
        )
    }

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
                )
            }

        return PageImpl(content, pageable, count)
    }
}