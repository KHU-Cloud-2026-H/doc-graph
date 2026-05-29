package com.docgraph.backend.document.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.IconResponse
import com.docgraph.backend.document.query.application.IconType
import com.docgraph.backend.document.query.application.NotionPageBrowser
import com.docgraph.backend.document.query.application.NotionPageRef
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.workspace.query.application.FindWorkspaceCreatorIdByIdQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

class FakeGetCurrentUserIdQueryN : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

class FakeFindWorkspaceCreatorIdByIdQuery : FindWorkspaceCreatorIdByIdQuery {
    @Volatile var creatorByWorkspace: (Long) -> Long? = { null }
    override fun find(workspaceId: Long): Long? = creatorByWorkspace(workspaceId)
}

class FakeNotionPageBrowser : NotionPageBrowser {
    @Volatile var roots: (Long) -> List<NotionPageRef> = { emptyList() }
    @Volatile var children: (Long, String) -> List<NotionPageRef> = { _, _ -> emptyList() }
    override fun listRootPages(workspaceId: Long): List<NotionPageRef> = roots(workspaceId)
    override fun listChildPages(workspaceId: Long, parentNotionPageId: String): List<NotionPageRef> =
        children(workspaceId, parentNotionPageId)
}

@TestConfiguration
class NotionPageQueryControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQueryN()
    @Bean @Primary fun fakeFindWorkspaceCreatorId() = FakeFindWorkspaceCreatorIdByIdQuery()
    @Bean @Primary fun fakeNotionPageBrowser() = FakeNotionPageBrowser()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(NotionPageQueryControllerTestConfig::class, SharedPostgresContainer::class)
class NotionPageQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val getCurrentUserId: FakeGetCurrentUserIdQueryN,
    private val findWorkspaceCreatorId: FakeFindWorkspaceCreatorIdByIdQuery,
    private val browser: FakeNotionPageBrowser,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
        findWorkspaceCreatorId.creatorByWorkspace = { null }
        browser.roots = { emptyList() }
        browser.children = { _, _ -> emptyList() }
    }

    @Test
    fun `GET notion-pages — 생성자 happy, 최상위 페이지 목록 + shape`() {
        findWorkspaceCreatorId.creatorByWorkspace = { 1L }
        browser.roots = {
            listOf(
                NotionPageRef(notionPageId = "p1", title = "Page 1", icon = IconResponse(IconType.EMOJI, "📄")),
                NotionPageRef(notionPageId = "p2", title = "Page 2", icon = null),
            )
        }

        mockMvc.get("/workspaces/10/notion-pages").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.notionPageId == 'p1')].title") { value("Page 1") }
            jsonPath("$[?(@.notionPageId == 'p1')].icon.type") { value("EMOJI") }
            jsonPath("$[?(@.notionPageId == 'p1')].icon.value") { value("📄") }
            jsonPath("$[?(@.notionPageId == 'p2')].icon") { value(null) }
        }
    }

    @Test
    fun `GET notion-pages — 호출자가 생성자 아님 → 403`() {
        findWorkspaceCreatorId.creatorByWorkspace = { 999L }
        getCurrentUserId.userId = 1L

        mockMvc.get("/workspaces/10/notion-pages").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET notion-pages — 워크스페이스 없음 → 404`() {
        findWorkspaceCreatorId.creatorByWorkspace = { null }

        mockMvc.get("/workspaces/10/notion-pages").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET notion-pages children — 생성자 happy, 직계 자식 목록 + shape`() {
        findWorkspaceCreatorId.creatorByWorkspace = { 1L }
        browser.children = { _, parent ->
            if (parent == "p1") {
                listOf(NotionPageRef(notionPageId = "c1", title = "Child 1", icon = IconResponse(IconType.EXTERNAL, "https://x/i.png")))
            } else {
                emptyList()
            }
        }

        mockMvc.get("/workspaces/10/notion-pages/p1/children").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].notionPageId") { value("c1") }
            jsonPath("$[0].title") { value("Child 1") }
            jsonPath("$[0].icon.type") { value("EXTERNAL") }
            jsonPath("$[0].icon.value") { value("https://x/i.png") }
        }
    }

    @Test
    fun `GET notion-pages children — 호출자가 생성자 아님 → 403`() {
        findWorkspaceCreatorId.creatorByWorkspace = { 999L }
        getCurrentUserId.userId = 1L

        mockMvc.get("/workspaces/10/notion-pages/p1/children").andExpect {
            status { isForbidden() }
        }
    }
}