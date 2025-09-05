package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.util.*

/**
 * Tag entity for flexible song labeling.
 * 
 * @property id Unique identifier for the tag
 * @property name Tag name (e.g., "Christmas", "Communion", "Easter")
 * @property churchId Reference to the church that owns this tag
 */
@Entity
@Table(name = "tags")
data class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 50)
    val name: String,
    
    @Column(nullable = false)
    val churchId: UUID
)