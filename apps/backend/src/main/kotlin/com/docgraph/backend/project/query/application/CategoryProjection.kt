package com.docgraph.backend.project.query.application

import com.docgraph.backend.document.query.application.DocumentType

/**
 * cross-domain용. document worker가 조상 라인 카테고리 매핑 조회 시 사용.
 */
data class CategoryProjection(
    val notionPageId: String,
    val documentType: DocumentType,
)
