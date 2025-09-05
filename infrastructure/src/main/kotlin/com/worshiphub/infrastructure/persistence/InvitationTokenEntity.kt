package com.worshiphub.infrastructure.persistence

import com.worshiphub.domain.auth.InvitationToken
import com.worshiphub.domain.organization.UserRole
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * JPA entity for invitation tokens.
 */
@Entity
@Table(
    name = "invitation_tokens",
    indexes = [
        Index(name = "idx_invitation_token", columnList = "token"),
        Index(name = "idx_invitation_email", columnList = "email"),
        Index(name = "idx_invitation_church_id", columnList = "church_id"),
        Index(name = "idx_invitation_expires_at", columnList = "expires_at")
    ]
)
data class InvitationTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true, length = 255)
    val token: String,
    
    @Column(nullable = false, length = 100)
    val email: String,
    
    @Column(name = "first_name", nullable = false, length = 50)
    val firstName: String,
    
    @Column(name = "last_name", nullable = false, length = 50)
    val lastName: String,
    
    @Column(name = "church_id", nullable = false)
    val churchId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole,
    
    @Column(name = "invited_by", nullable = false)
    val invitedBy: UUID,
    
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
    
    @Column(name = "is_used", nullable = false)
    val isUsed: Boolean = false,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Converts to domain model.
     */
    fun toDomain(): InvitationToken = InvitationToken(
        id = id,
        token = token,
        email = email,
        firstName = firstName,
        lastName = lastName,
        churchId = churchId,
        role = role,
        invitedBy = invitedBy,
        expiresAt = expiresAt,
        isUsed = isUsed,
        createdAt = createdAt
    )
    
    companion object {
        /**
         * Creates entity from domain model.
         */
        fun fromDomain(domain: InvitationToken): InvitationTokenEntity = 
            InvitationTokenEntity(
                id = domain.id,
                token = domain.token,
                email = domain.email,
                firstName = domain.firstName,
                lastName = domain.lastName,
                churchId = domain.churchId,
                role = domain.role,
                invitedBy = domain.invitedBy,
                expiresAt = domain.expiresAt,
                isUsed = domain.isUsed,
                createdAt = domain.createdAt
            )
    }
}