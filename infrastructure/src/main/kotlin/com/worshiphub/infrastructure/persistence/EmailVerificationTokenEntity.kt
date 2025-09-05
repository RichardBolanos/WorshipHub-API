package com.worshiphub.infrastructure.persistence

import com.worshiphub.domain.auth.EmailVerificationToken
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * JPA entity for email verification tokens.
 */
@Entity
@Table(
    name = "email_verification_tokens",
    indexes = [
        Index(name = "idx_email_verification_token", columnList = "token"),
        Index(name = "idx_email_verification_user_id", columnList = "user_id"),
        Index(name = "idx_email_verification_expires_at", columnList = "expires_at")
    ]
)
data class EmailVerificationTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, unique = true, length = 255)
    val token: String,
    
    @Column(nullable = false, length = 100)
    val email: String,
    
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
    fun toDomain(): EmailVerificationToken = EmailVerificationToken(
        id = id,
        userId = userId,
        token = token,
        email = email,
        expiresAt = expiresAt,
        isUsed = isUsed,
        createdAt = createdAt
    )
    
    companion object {
        /**
         * Creates entity from domain model.
         */
        fun fromDomain(domain: EmailVerificationToken): EmailVerificationTokenEntity = 
            EmailVerificationTokenEntity(
                id = domain.id,
                userId = domain.userId,
                token = domain.token,
                email = domain.email,
                expiresAt = domain.expiresAt,
                isUsed = domain.isUsed,
                createdAt = domain.createdAt
            )
    }
}