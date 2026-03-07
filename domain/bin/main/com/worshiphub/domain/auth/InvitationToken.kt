package com.worshiphub.domain.auth

import com.worshiphub.domain.organization.UserRole
import jakarta.persistence.*
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
@Entity
@Table(name = "invitation_tokens")
data class InvitationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true)
    val token: String,
    
    @Column(nullable = false)
    val email: String,
    
    @Column(nullable = false)
    val firstName: String,
    
    @Column(nullable = false)
    val lastName: String,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,
    
    @Column(nullable = false)
    val invitedBy: UUID,
    
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
    
    @Column(nullable = false)
    val isUsed: Boolean = false,
    
    @Column(nullable = false)
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