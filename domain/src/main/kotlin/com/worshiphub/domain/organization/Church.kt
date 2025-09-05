package com.worshiphub.domain.organization

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
data class Church(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val address: String,
    val email: String,
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