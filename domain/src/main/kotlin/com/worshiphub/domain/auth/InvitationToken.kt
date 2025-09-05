package com.worshiphub.domain.auth

import com.worshiphub.domain.organization.UserRole
import java.time.LocalDateTime
import java.util.*

/**
 * Domain entity representing an invitation token.
 * 
 * @property id Unique identifier for the invitation
 * @property token Secure invitation token
 * @property email Email address being invited
 * @property firstName First name of invitee
 * @property lastName Last name of invitee
 * @property churchId Church the user is being invited to
 * @property role Role being assigned to the invitee
 * @property invitedBy User ID who sent the invitation
 * @property expiresAt Token expiration timestamp
 * @property isUsed Whether the invitation has been accepted
 * @property createdAt Invitation creation timestamp
 */
data class InvitationToken(
    val id: UUID = UUID.randomUUID(),
    val token: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val churchId: UUID,
    val role: UserRole,
    val invitedBy: UUID,
    val expiresAt: LocalDateTime,
    val isUsed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        /**
         * Invitation tokens expire in 7 days.
         */
        const val EXPIRATION_DAYS = 7L
    }
    
    /**
     * Checks if the invitation is valid (not expired and not used).
     */
    fun isValid(): Boolean = !isUsed && LocalDateTime.now().isBefore(expiresAt)
    
    /**
     * Marks the invitation as used.
     */
    fun markAsUsed(): InvitationToken = copy(isUsed = true)
    
    /**
     * Checks if the invitation is expired.
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
}