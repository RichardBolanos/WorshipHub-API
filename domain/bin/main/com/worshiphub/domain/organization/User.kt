package com.worshiphub.domain.organization

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * User aggregate representing a person in the worship platform.
 * 
 * @property id Unique identifier for the user
 * @property email User's email address (unique)
 * @property firstName User's first name
 * @property lastName User's last name
 * @property passwordHash Hashed password for authentication
 * @property churchId Reference to the church this user belongs to
 * @property role Global role within the church organization
 * @property isActive Whether the user account is active
 * @property isEmailVerified Whether the user's email has been verified
 * @property createdAt Timestamp when the user was created
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true, length = 100)
    val email: String,
    
    @Column(nullable = false, length = 50)
    val firstName: String,
    
    @Column(nullable = false, length = 50)
    val lastName: String,
    
    @Column(nullable = true)
    val passwordHash: String? = null,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val isEmailVerified: Boolean = false,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "User(id=$id, email='$email', role=$role)"
    }
}

