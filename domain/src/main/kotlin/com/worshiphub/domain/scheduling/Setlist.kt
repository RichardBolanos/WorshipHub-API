package com.worshiphub.domain.scheduling

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Setlist entity representing a collection of songs for a service.
 * 
 * @property id Unique identifier for the setlist
 * @property name Name of the setlist
 * @property songIds Ordered list of song IDs in the setlist
 * @property estimatedDuration Total estimated duration in minutes
 * @property churchId Reference to the church that owns this setlist
 * @property createdAt Timestamp when the setlist was created
 */
@Entity
@Table(name = "setlists")
data class Setlist(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    @ElementCollection
    @CollectionTable(name = "setlist_songs", joinColumns = [JoinColumn(name = "setlist_id")])
    @OrderColumn(name = "song_order")
    @Column(name = "song_id")
    val songIds: List<UUID> = emptyList(),
    
    @Column
    val estimatedDuration: Int? = null,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)