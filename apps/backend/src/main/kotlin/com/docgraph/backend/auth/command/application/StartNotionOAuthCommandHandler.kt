package com.docgraph.backend.auth.command.application

import com.docgraph.backend.auth.command.domain.NotionOAuthAuthorizationUrlProvider
import org.springframework.stereotype.Service

@Service
class StartNotionOAuthCommandHandler(
    private val authorizationUrlProvider: NotionOAuthAuthorizationUrlProvider,
) {
    fun handle(command: StartNotionOAuthCommand): String =
        authorizationUrlProvider.authorizationUrl(command.state)
}
