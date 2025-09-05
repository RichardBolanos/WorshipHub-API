package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * GlobalSong entity for the global song catalog.
 * 
 * @property id Unique identifier for the global song
 * @property title Song title
 * @property artist Original artist or composer
 * @property key Default musical key
 * @property bpm Default beats per minute
 * @property chords Official chords in ChordPro format
 * @property isVerified Whether the song is officially verified
 * @property createdAt Timestamp when added to global catalog
 */
@Entity
@Table(name = "global_songs")
data class GlobalSong(
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
    val isVerified: Boolean = false,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)