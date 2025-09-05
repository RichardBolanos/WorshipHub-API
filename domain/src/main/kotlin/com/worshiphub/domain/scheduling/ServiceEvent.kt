package com.worshiphub.domain.scheduling

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * ServiceEvent aggregate root representing a scheduled worship service.
 * 
 * @property id Unique identifier for the service event
 * @property name Name of the service (e.g., "Sunday Morning Service")
 * @property scheduledDate Date and time when the service is scheduled
 * @property teamId Reference to the team assigned to this service
 * @property setlistId Reference to the setlist for this service
 * @property churchId Reference to the church hosting this service
 * @property createdAt Timestamp when the service event was created
 */
@Entity
@Table(name = "service_events")
data class ServiceEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    @Column(nullable = false)
    val scheduledDate: LocalDateTime,
    
    @Column(nullable = false)
    val teamId: UUID,
    
    @Column
    val setlistId: UUID? = null,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)