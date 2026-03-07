package com.worshiphub.domain.organization

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Church aggregate root representing a worship organization.
 * 
 * @property id Unique identifier for the church
 * @property name Official name of the church
 * @property address Physical address of the church
 * @property email Contact email for the church
 * @property createdAt Timestamp when the church was registered
 */
@Entity
@Table(name = "churches")
data class Church(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 200)
    val name: String,
    
    @Column(nullable = false, length = 500)
    val address: String,
    
    @Column(nullable = false, unique = true, length = 100)
    val email: String,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Church) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Church(id=$id, name='$name', email='$email')"
    }
}