package com.worshiphub.application.auth

import com.worshiphub.domain.organization.UserRole
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Service for audit logging of security-related events.
 */
@Service
class AuditService {
    
    private val logger = LoggerFactory.getLogger(AuditService::class.java)
    
    /**
     * Logs role change events.
     */
    fun logRoleChange(
        userId: UUID,
        oldRole: UserRole,
        newRole: UserRole,
        changedBy: UUID,
        reason: String?
    ) {
        val auditEntry = mapOf(
            "event" to "ROLE_CHANGE",
            "timestamp" to LocalDateTime.now().toString(),
            "userId" to userId.toString(),
            "oldRole" to oldRole.name,
            "newRole" to newRole.name,
            "changedBy" to changedBy.toString(),
            "reason" to (reason ?: "No reason provided")
        )
        
        logger.info("AUDIT: Role change - User: {}, {} -> {}, Changed by: {}", userId, oldRole.name, newRole.name, changedBy)
        
        // TODO: In production, store in dedicated audit table or external audit system
    }
    
    /**
     * Logs authentication events.
     */
    fun logAuthenticationEvent(
        event: String,
        userId: UUID?,
        email: String?,
        success: Boolean,
        details: String? = null
    ) {
        val auditEntry = mapOf(
            "event" to event,
            "timestamp" to LocalDateTime.now().toString(),
            "userId" to userId?.toString(),
            "email" to email,
            "success" to success.toString(),
            "details" to details
        )
        
        logger.info("AUDIT: Authentication - Event: {}, Email: {}, Success: {}", event, sanitizeForLog(email), success)
        
        // TODO: In production, store in dedicated audit table or external audit system
    }
    
    /**
     * Logs invitation events.
     */
    fun logInvitationEvent(
        event: String,
        invitationId: UUID,
        email: String,
        invitedBy: UUID,
        churchId: UUID
    ) {
        val auditEntry = mapOf(
            "event" to event,
            "timestamp" to LocalDateTime.now().toString(),
            "invitationId" to invitationId.toString(),
            "email" to email,
            "invitedBy" to invitedBy.toString(),
            "churchId" to churchId.toString()
        )
        
        logger.info("AUDIT: Invitation - Event: {}, Email: {}, Invited by: {}", event, sanitizeForLog(email), invitedBy)
        
        // TODO: In production, store in dedicated audit table or external audit system
    }
    
    private fun sanitizeForLog(input: String?): String? {
        return input?.replace("[\r\n\t]".toRegex(), "_")
    }
}