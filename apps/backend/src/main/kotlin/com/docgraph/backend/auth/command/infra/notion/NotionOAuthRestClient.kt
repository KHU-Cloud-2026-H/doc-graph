package com.docgraph.backend.auth.command.infra.notion

import com.docgraph.backend.auth.command.domain.NotionOAuthClient
import com.docgraph.backend.auth.command.domain.NotionOAuthToken
import com.docgraph.backend.auth.command.domain.NotionOAuthUser
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.Base64

@Component
class NotionOAuthRestClient(
    private val registration: NotionOAuthRegistration,
) : NotionOAuthClient {
    private val restClient = RestClient.builder().build()

    override fun exchangeAuthorizationCode(code: String): NotionOAuthToken {
        val response = restClient.post()
            .uri(registration.tokenUri)
            .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
            .contentType(MediaType.APPLICATION_JSON)
            .body(NotionTokenRequest(code = code, redirectUri = registration.redirectUri))
            .retrieve()
            .body<NotionTokenResponse>()
            ?: error("Notion OAuth token response body is null")
        return response.toDomain()
    }

    private fun basicAuthorization(): String {
        val credential = "${registration.clientId}:${registration.clientSecret}"
        return "Basic " + Base64.getEncoder().encodeToString(credential.toByteArray(Charsets.UTF_8))
    }
}

data class NotionTokenRequest(
    @JsonProperty("grant_type")
    val grantType: String = "authorization_code",
    val code: String,
    @JsonProperty("redirect_uri")
    val redirectUri: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("bot_id")
    val botId: String,
    @JsonProperty("workspace_id")
    val workspaceId: String,
    @JsonProperty("workspace_name")
    val workspaceName: String?,
    val owner: NotionOwnerResponse,
) {
    fun toDomain(): NotionOAuthToken {
        val user = owner.user ?: error("Notion OAuth owner must be user")
        val email = user.person?.email ?: error("Notion OAuth user email is missing")
        return NotionOAuthToken(
            accessToken = accessToken,
            tokenType = tokenType,
            botId = botId,
            workspaceId = workspaceId,
            workspaceName = workspaceName ?: "Notion Workspace",
            owner = NotionOAuthUser(
                notionUserId = user.id,
                email = email,
                name = user.name,
                avatarUrl = user.avatarUrl,
            ),
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionOwnerResponse(
    val type: String,
    val user: NotionUserResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionUserResponse(
    val id: String,
    val name: String,
    @JsonProperty("avatar_url")
    val avatarUrl: String? = null,
    val person: NotionPersonResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionPersonResponse(
    val email: String,
)
