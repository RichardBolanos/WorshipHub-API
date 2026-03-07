package com.worshiphub.domain.scheduling

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * UserAvailability entity for tracking when users are not available.
 * 
 * @property id Unique identifier for the availability record
 * @property userId Reference to the user
 * @property unavailableDate Date when the user is not available
 * @property reason Optional reason for unavailability
 * @property createdAt Timestamp when the availability was recorded
 */
@Entity
@Table(name = "user_availability")
data class UserAvailability(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false)
    val unavailableDate: LocalDate,
    
    @Column(length = 200)
    val reason: String? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * Updates the reason for unavailability.
     */
    fun updateReason(newReason: String?): UserAvailability {
        return copy(reason = newReason?.take(200)) // Limit to 200 characters
    }
    
    /**
     * Checks if this unavailability is for a future date.
     */
    fun isFuture(): Boolean {
        return unavailableDate.isAfter(LocalDate.now())
    }
    
    /**
     * Checks if this unavailability conflicts with a given date.
     */
    fun conflictsWith(date: LocalDate): Boolean {
        return unavailableDate == date
    }
    
    /**
     * Checks if this unavailability is within a date range.
     */
    fun isWithinRange(startDate: LocalDate, endDate: LocalDate): Boolean {
        return !unavailableDate.isBefore(startDate) && !unavailableDate.isAfter(endDate)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserAvailability) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "UserAvailability(id=$id, userId=$userId, unavailableDate=$unavailableDate)"
    }
}