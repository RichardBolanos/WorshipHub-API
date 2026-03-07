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
) {
    
    /**
     * Adds a song to the end of the setlist.
     */
    fun addSong(songId: UUID): Setlist {
        val updatedSongs = songIds + songId
        return copy(songIds = updatedSongs)
    }
    
    /**
     * Inserts a song at a specific position in the setlist.
     */
    fun insertSong(songId: UUID, position: Int): Setlist {
        require(position >= 0 && position <= songIds.size) { "Invalid position: $position" }
        val updatedSongs = songIds.toMutableList().apply {
            add(position, songId)
        }
        return copy(songIds = updatedSongs)
    }
    
    /**
     * Removes a song from the setlist.
     */
    fun removeSong(songId: UUID): Setlist {
        val updatedSongs = songIds.filterNot { it == songId }
        return copy(songIds = updatedSongs)
    }
    
    /**
     * Reorders songs in the setlist.
     */
    fun reorderSongs(newOrder: List<UUID>): Setlist {
        require(newOrder.size == songIds.size) { "New order must contain all songs" }
        require(newOrder.toSet() == songIds.toSet()) { "New order must contain the same songs" }
        return copy(songIds = newOrder)
    }
    
    /**
     * Updates the estimated duration.
     */
    fun updateDuration(durationMinutes: Int): Setlist {
        require(durationMinutes > 0) { "Duration must be positive" }
        return copy(estimatedDuration = durationMinutes)
    }
    
    /**
     * Checks if the setlist is empty.
     */
    fun isEmpty(): Boolean = songIds.isEmpty()
    
    /**
     * Gets the number of songs in the setlist.
     */
    fun songCount(): Int = songIds.size
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Setlist) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Setlist(id=$id, name='$name', songCount=${songIds.size})"
    }
}