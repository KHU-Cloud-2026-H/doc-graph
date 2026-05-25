package com.docgraph.backend.validation.query.application

/**
 * conflict의 대표(최신) finding title을 조회한다. notification 알림 메시지 요약에 사용.
 * finding이 없으면 null.
 */
fun interface FindConflictTitleByIdQuery {
    fun find(conflictId: Long): String?
}
