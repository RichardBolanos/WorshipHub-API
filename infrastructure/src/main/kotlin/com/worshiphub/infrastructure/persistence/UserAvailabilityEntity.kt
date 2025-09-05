package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "user_availability")
data class UserAvailabilityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false)
    val unavailableDate: LocalDate,
    
    @Column(length = 200)
    val reason: String?,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)