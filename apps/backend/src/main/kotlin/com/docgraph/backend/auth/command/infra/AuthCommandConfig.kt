package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.application.AuthSessionProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AuthSessionProperties::class,
)
class AuthCommandConfig
