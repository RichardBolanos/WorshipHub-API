package com.worshiphub.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // Allow all localhost ports (Flutter Web dev server can run on any port)
        // and production domains
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.worshiphub.com"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Church-Id",          // Required by all API calls in WorshipHub
            "Upgrade",            // Required for WebSocket handshake
            "Connection",         // Required for WebSocket handshake
            "Sec-WebSocket-Key",
            "Sec-WebSocket-Version",
            "Sec-WebSocket-Extensions"
        )
        configuration.allowCredentials = true
        configuration.maxAge = 3600L // Cache preflight for 1 hour

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
