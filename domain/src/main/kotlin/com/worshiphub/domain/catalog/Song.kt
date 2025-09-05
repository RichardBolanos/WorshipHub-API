package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Song aggregate root representing a worship song in the catalog.
 * 
 * @property id Unique identifier for the song
 * @property title Song title
 * @property artist Original artist or composer
 * @property key Musical key (e.g., "C", "G", "Am")
 * @property bpm Beats per minute (tempo)
 * @property chords Song chords in ChordPro format
 * @property churchId Reference to the church that owns this song
 * @property createdAt Timestamp when the song was added
 */
@Entity
@Table(name = "songs")
data class Song(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(nullable = false, length = 100)
    val artist: String,
    
    @Column(name = "song_key", nullable = false, length = 10)
    val key: String,
    
    @Column
    val bpm: Int? = null,
    
    @Column(columnDefinition = "TEXT")
    val chords: String? = null,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    
    fun transpose(toKey: String): Song {
        val transposedChords = chords?.let { 
            ChordTransposer.transpose(it, key, toKey) 
        }
        return copy(key = toKey, chords = transposedChords)
    }
    
    fun addDuration(durationMinutes: Int): Song {
        // Business logic for adding duration
        return this
    }
}