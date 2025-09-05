package com.worshiphub.infrastructure.persistence

import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true, length = 100)
    val email: String,
    
    @Column(nullable = false, length = 50)
    val firstName: String,
    
    @Column(nullable = false, length = 50)
    val lastName: String,
    
    @Column(nullable = false, length = 255)
    val passwordHash: String,
    
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
    fun toDomain(): User = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        passwordHash = passwordHash,
        churchId = churchId,
        role = role,
        isActive = isActive,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt
    )
    
    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            passwordHash = user.passwordHash,
            churchId = user.churchId,
            role = user.role,
            isActive = user.isActive,
            isEmailVerified = user.isEmailVerified,
            createdAt = user.createdAt
        )
    }
}