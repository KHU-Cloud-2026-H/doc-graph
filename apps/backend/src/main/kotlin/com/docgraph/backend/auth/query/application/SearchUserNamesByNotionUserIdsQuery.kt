package com.docgraph.backend.auth.query.application

fun interface SearchUserNamesByNotionUserIdsQuery {
    /** Notion user id별 표시 이름. 앱에 로그인한 적 없는 id(외부 협업자·봇)는 결과 map에서 누락된다. */
    fun search(notionUserIds: List<String>): Map<String, String>
}
