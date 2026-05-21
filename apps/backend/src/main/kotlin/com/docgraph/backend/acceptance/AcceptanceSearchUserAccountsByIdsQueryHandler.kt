package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.auth.query.application.UserResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * acceptance profile 한정 [SearchUserAccountsByIdsQuery] override — auth 영속 미구현 동안의 우회.
 *
 * 입력 userId마다 placeholder UserResponse 반환. acceptance spec이 GET /workspaces/{id}로 멤버 정보
 * 조회 시 동작 가능하게 함. 운영 stub은 throw (`SearchUserAccountsByIdsQueryHandler`).
 *
 * 교체 조건: auth User entity 영속화 완성 시 운영 Handler 구현 + 본 빈 제거.
 */
@Component
@Profile("acceptance")
class AcceptanceSearchUserAccountsByIdsQueryHandler : SearchUserAccountsByIdsQuery {
    override fun search(userIds: List<Long>): List<UserResponse> =
        userIds.map { id ->
            UserResponse(
                id = id,
                email = "user-$id@example.com",
                name = "User $id",
            )
        }
}