package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.util.*

/**
 * Category entity for organizing songs by type.
 * 
 * @property id Unique identifier for the category
 * @property name Category name (e.g., "Worship", "Joy", "Prayer")
 * @property description Optional description of the category
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
    
    @Column(length = 200)
    val description: String? = null,
    
    @Column(nullable = false)
    val churchId: UUID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Category) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Category(id=$id, name='$name')"
    }
}