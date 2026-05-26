package com.docgraph.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    @Value("\${cors.allowed-origin}")
    private val corsAllowedOrigin: String,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
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

        return http.build()
    }

    /**
     * 프론트엔드가 다른 origin에서 세션 쿠키로 호출할 때 필요. allowCredentials=true라 패턴 매칭
     * (allowedOriginPatterns) — `*`는 요청 origin을 반사한다(스펙상 allowedOrigins엔 `*`+credentials 불가).
     * 값은 CORS_ALLOWED_ORIGIN env 단일 출처. demo는 `*`, 배포 시 실제 프론트 origin.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf(corsAllowedOrigin)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
