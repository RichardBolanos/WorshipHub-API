package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.util.*

/**
 * Tag entity for flexible song labeling.
 * 
 * @property id Unique identifier for the tag
 * @property name Tag name (e.g., "Christmas", "Communion", "Easter")
 * @property color Optional color for visual organization
 * @property songId Reference to the song this tag belongs to
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
    
    @Column(length = 7) // Hex color code
    val color: String? = null,
    
    @Column(nullable = false)
    val songId: UUID,
    
    @Column(nullable = false)
    val churchId: UUID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Tag(id=$id, name='$name')"
    }
}