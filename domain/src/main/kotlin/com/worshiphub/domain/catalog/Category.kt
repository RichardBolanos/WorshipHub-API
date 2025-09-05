package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.util.*

/**
 * Category entity for organizing songs by type.
 * 
 * @property id Unique identifier for the category
 * @property name Category name (e.g., "Worship", "Joy", "Prayer")
 * @property churchId Reference to the church that owns this category
 */
@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 50)
    val name: String,
    
    @Column(nullable = false)
    val churchId: UUID
)