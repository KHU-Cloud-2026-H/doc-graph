package com.docgraph.backend.document.query.application

import java.time.OffsetDateTime

data class DocumentReference(
    val id: Long,
    val projectId: Long,
    val title: String,
    val type: DocumentType?,
    // Notion 최종 수정 시각. 승인 시 lost-update 가드의 낙관적 동시성 토큰으로 client가 echo. 미동기화 시 null.
    val notionLastEditedAt: OffsetDateTime?,
)
