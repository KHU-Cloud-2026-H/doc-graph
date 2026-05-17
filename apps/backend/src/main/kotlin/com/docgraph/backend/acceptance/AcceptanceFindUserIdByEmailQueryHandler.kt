package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.FindUserIdByEmailQuery
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * acceptance profile 한정 [FindUserIdByEmailQuery] override — auth 영속 미구현 동안의 우회.
 *
 * `AcceptanceSearchUserAccountsByIdsQueryHandler`와 짝 — 그쪽이 userId N → `user-N@example.com` 로
 * 풀어주므로, 본 Handler는 역방향으로 `user-N@example.com` → N. 패턴 불일치 email은 null (운영의
 * "미가입 사용자" 의미를 보존).
 *
 * 교체 조건: auth User entity 영속화 완성 시 본 빈 제거 → 운영 Handler가 active.
 */
@Component
@Profile("acceptance")
class AcceptanceFindUserIdByEmailQueryHandler : FindUserIdByEmailQuery {
    override fun find(email: String): Long? = PATTERN.matchEntire(email)?.groupValues?.get(1)?.toLong()

    companion object {
        private val PATTERN = Regex("""^user-(\d+)@example\.com$""")
    }
}
