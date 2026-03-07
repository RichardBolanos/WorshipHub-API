package com.worshiphub.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Enhanced security configuration for production readiness.
 */
@Configuration
class SecurityEnhancementConfig : WebMvcConfigurer {

    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        val loggingFilter = CommonsRequestLoggingFilter()
        loggingFilter.setIncludeClientInfo(true)
        loggingFilter.setIncludeQueryString(true)
        loggingFilter.setIncludePayload(false) // Security: don't log request bodies
        loggingFilter.setIncludeHeaders(false) // Security: don't log headers (may contain tokens)
        loggingFilter.setMaxPayloadLength(0)
        return loggingFilter
    }
}

/**
 * Security headers filter for enhanced protection.
 */
@Component
@Order(1)
class SecurityHeadersFilter : Filter {
    
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpResponse = response as HttpServletResponse
        
        // Security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff")
        httpResponse.setHeader("X-Frame-Options", "DENY")
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block")
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
        httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        
        // Content Security Policy
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self'; " +
            "font-src 'self'; " +
            "object-src 'none'; " +
            "media-src 'self'; " +
            "frame-src 'none';"
        )
        
        chain.doFilter(request, response)
    }
}