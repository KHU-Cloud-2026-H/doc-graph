package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.config.ConditionalOnRealHttpAdapter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(OpenAiProperties::class)
class OpenAiAdapterConfig {

    @Bean
    @ConditionalOnRealHttpAdapter
    fun openAiRestClient(props: OpenAiProperties): RestClient {
        val timeout = Duration.ofMillis(props.timeoutMs)
        val httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build()
        val factory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(timeout)
        }
        return RestClient.builder()
            .baseUrl(props.baseUrl)
            .requestFactory(factory)
            .defaultHeader("Authorization", "Bearer ${props.apiKey}")
            .build()
    }
}
