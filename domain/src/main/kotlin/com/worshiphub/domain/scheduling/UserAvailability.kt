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
)