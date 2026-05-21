package com.docgraph.backend.auth.command.infra.notion

import com.docgraph.backend.auth.command.domain.NotionOAuthAuthorizationUrlProvider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.UriComponentsBuilder

@Component
class NotionOAuthRegistration(
    private val clientRegistrationRepository: ClientRegistrationRepository,
) : NotionOAuthAuthorizationUrlProvider {
    val clientId: String
        get() = registration.clientId

    val clientSecret: String
        get() = registration.clientSecret

    val tokenUri: String
        get() = registration.providerDetails.tokenUri

    val redirectUri: String
        get() = resolveRedirectUri()

    override fun authorizationUrl(state: String?): String {
        val builder = UriComponentsBuilder.fromUriString(registration.providerDetails.authorizationUri)
            .queryParam("client_id", registration.clientId)
            .queryParam("response_type", "code")
            .queryParam("owner", "user")
            .queryParam("redirect_uri", resolveRedirectUri())
        if (!state.isNullOrBlank()) {
            builder.queryParam("state", state)
        }
        return builder.build().encode().toUriString()
    }

    private fun resolveRedirectUri(): String {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: error("current HTTP request is required to resolve OAuth2 redirect URI")
        val baseUrl = UriComponentsBuilder.newInstance()
            .scheme(request.scheme)
            .host(request.serverName)
            .apply {
                if (!request.isDefaultPort()) {
                    port(request.serverPort)
                }
            }
            .path(request.contextPath)
            .build()
            .toUriString()
        return registration.redirectUri
            .replace("{baseScheme}", request.scheme)
            .replace("{baseHost}", request.serverName)
            .replace("{basePort}", if (request.isDefaultPort()) "" else ":${request.serverPort}")
            .replace("{basePath}", request.contextPath)
            .replace("{baseUrl}", baseUrl)
            .replace("{registrationId}", REGISTRATION_ID)
    }

    private fun jakarta.servlet.http.HttpServletRequest.isDefaultPort(): Boolean =
        (scheme == "http" && serverPort == 80) || (scheme == "https" && serverPort == 443)

    private val registration: ClientRegistration
        get() = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)
            ?: error("OAuth2 client registration '$REGISTRATION_ID' is not configured")

    companion object {
        private const val REGISTRATION_ID = "notion"
    }
}
