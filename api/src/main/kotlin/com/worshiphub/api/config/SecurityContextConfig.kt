package com.worshiphub.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.annotation.RequestScope
import java.util.*

@Configuration
class SecurityContextConfig {
    
    fun currentUserId(): UUID? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.isAuthenticated == true && authentication.name != "anonymousUser") {
            try {
                UUID.fromString(authentication.name)
            } catch (e: IllegalArgumentException) {
                null
            }
        } else {
            null
        }
    }
    
    @Bean
    @RequestScope
    fun currentUserRoles(): List<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.map { it.authority } ?: emptyList()
    }
}