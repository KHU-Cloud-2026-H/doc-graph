package com.docgraph.backend.document.query.application

/**
 * Notion API 라이브 조회 포트 — 프로젝트 생성 위저드의 페이지 브라우징.
 *
 * 영속된 Document가 아니라 Notion API를 호출 시점에 직접 조회한다(생성 시점엔 동기화된 문서가 없음).
 * 운영 어댑터(Notion API client)가 cursor 페이지네이션 fetch-all·상한 cap·워크스페이스 토큰 해소를 처리한다.
 */
interface NotionPageBrowser {
    /** 워크스페이스 최상위(parent = workspace) 페이지 목록. */
    fun listRootPages(workspaceId: Long): List<NotionPageRef>

    /** 주어진 페이지의 직계 자식 페이지 목록. */
    fun listChildPages(workspaceId: Long, parentNotionPageId: String): List<NotionPageRef>
}
