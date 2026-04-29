package com.docgraph.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/oauth2/authorization/**",
                        "/login/oauth2/code/**",
                        "/webhooks/**",
                    ).permitAll()
                    .anyRequest().permitAll()
            }
            .csrf { it.disable() }
            .oauth2Login { }

        return http.build()
    }
}
