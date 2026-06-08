package com.docgraph.backend.auth.query.infra

import com.docgraph.backend.auth.command.domain.UserAccount
import com.docgraph.backend.auth.command.infra.UserAccountJpaRepository
import com.docgraph.backend.fixtures.SharedPostgresContainer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, AuthQueryRepository::class)
class AuthQueryRepositoryTest @Autowired constructor(
    private val queryRepository: AuthQueryRepository,
    private val userAccountJpaRepository: UserAccountJpaRepository,
) {

    private fun saveUser(notionUserId: String, name: String) {
        userAccountJpaRepository.save(
            UserAccount.registerFromNotion(
                notionUserId = notionUserId,
                email = "$notionUserId@example.com",
                name = name,
                avatarUrl = null,
            ),
        )
    }

    @Test
    fun `searchUserNamesByNotionUserIds — notion user id별 이름을 map으로 반환`() {
        saveUser("notion-user-1", "홍길동")
        saveUser("notion-user-2", "김철수")

        val result = queryRepository.searchUserNamesByNotionUserIds(listOf("notion-user-1", "notion-user-2"))

        assertEquals(mapOf("notion-user-1" to "홍길동", "notion-user-2" to "김철수"), result)
    }

    @Test
    fun `searchUserNamesByNotionUserIds — 매칭 안 되는 id는 map에서 누락`() {
        saveUser("notion-user-1", "홍길동")

        val result = queryRepository.searchUserNamesByNotionUserIds(listOf("notion-user-1", "notion-user-unknown"))

        assertEquals(mapOf("notion-user-1" to "홍길동"), result)
    }

    @Test
    fun `searchUserNamesByNotionUserIds — 입력 빈 리스트면 빈 map`() {
        saveUser("notion-user-1", "홍길동")

        assertTrue(queryRepository.searchUserNamesByNotionUserIds(emptyList()).isEmpty())
    }

    @Test
    fun `searchUserNamesByNotionUserIds — 매칭 전무면 빈 map`() {
        saveUser("notion-user-1", "홍길동")

        assertTrue(queryRepository.searchUserNamesByNotionUserIds(listOf("none-a", "none-b")).isEmpty())
    }
}
