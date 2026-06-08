package com.docgraph.backend.document.query.application

import com.docgraph.backend.auth.query.application.SearchUserNamesByNotionUserIdsQuery
import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class FindDocumentByIdQueryHandler(
    private val repository: DocumentQueryRepository,
    private val searchUserNamesByNotionUserIds: SearchUserNamesByNotionUserIdsQuery,
) : FindDocumentByIdQuery {
    override fun find(documentId: Long): DocumentDetail? {
        val detail = repository.findDetail(documentId) ?: return null
        val editorNotionUserId = repository.findNotionLastEditedById(documentId)
            ?: return detail
        val name = searchUserNamesByNotionUserIds.search(listOf(editorNotionUserId))[editorNotionUserId]
        return detail.copy(lastEditedByName = name)
    }
}
