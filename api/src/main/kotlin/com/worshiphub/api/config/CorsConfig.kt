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
        // Secure CORS - only allow specific origins in production
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:3000",
            "http://localhost:8080", 
            "https://*.worshiphub.com"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept"
        )
        configuration.allowCredentials = true
        configuration.maxAge = 3600L // Cache preflight for 1 hour

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}