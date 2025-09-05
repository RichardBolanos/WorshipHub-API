package com.worshiphub.domain.organization

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
data class User(
    val id: UUID = UUID.randomUUID(),
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String,
    val churchId: UUID,
    val role: UserRole,
    val isActive: Boolean = true,
    val isEmailVerified: Boolean = false,
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

