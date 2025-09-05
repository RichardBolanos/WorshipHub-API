package com.worshiphub.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*

/**
 * Utility class for extracting user context from Spring Security.
 */
@Component
class SecurityContext {

    /**
     * Gets the current authenticated user ID.
     */
    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication context")
        return if (authentication.principal is String) {
            UUID.fromString(authentication.principal as String)
        } else {
            throw IllegalStateException("Invalid authentication principal")
        }
    }

    /**
     * Gets the current user's church ID.
     */
    fun getCurrentChurchId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication context")
        val details = authentication.details as? Map<*, *>
            ?: throw IllegalStateException("No authentication details")
        return details["churchId"]?.let { UUID.fromString(it.toString()) }
            ?: throw IllegalStateException("No church ID in authentication details")
    }

    /**
     * Checks if the current user belongs to the specified church.
     */
    fun belongsToChurch(churchId: UUID): Boolean {
        return getCurrentChurchId() == churchId
    }

    /**
     * Checks if the current user is authenticated.
     */
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }
}