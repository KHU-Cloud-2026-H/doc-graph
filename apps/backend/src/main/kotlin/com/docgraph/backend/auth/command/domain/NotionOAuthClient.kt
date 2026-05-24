package com.docgraph.backend.auth.command.domain

interface NotionOAuthClient {
    fun exchangeAuthorizationCode(code: String): NotionOAuthToken
}

interface NotionOAuthAuthorizationUrlProvider {
    fun authorizationUrl(state: String? = null): String
}

data class NotionOAuthToken(
    val accessToken: String,
    val tokenType: String,
    val botId: String,
    val workspaceId: String,
    val workspaceName: String,
    val owner: NotionOAuthUser,
)

data class NotionOAuthUser(
    val notionUserId: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
)
