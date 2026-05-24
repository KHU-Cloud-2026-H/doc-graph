package com.docgraph.backend.document.query.application

fun interface SearchDocumentReferencesByIdsQuery {
    fun search(documentIds: List<Long>): List<DocumentReference>
}
