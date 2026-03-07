package com.worshiphub.api.config

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.stereotype.Component

@Component
class SecurityAuditConfig {
    
    private val logger = LoggerFactory.getLogger(SecurityAuditConfig::class.java)
    
    @EventListener
    fun handleAuthenticationSuccess(event: AuthenticationSuccessEvent) {
        logger.info("Authentication successful for user: {}", event.authentication.name)
    }
    
    @EventListener
    fun handleAuthenticationFailure(event: AuthenticationFailureBadCredentialsEvent) {
        logger.warn("Authentication failed for user: {}", event.authentication.name)
    }
}