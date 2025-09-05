package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "global_songs")
data class GlobalSongEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(nullable = false, length = 100)
    val artist: String,
    
    @Column(nullable = false, length = 10)
    val songKey: String,
    
    val bpm: Int?,
    
    @Column(columnDefinition = "TEXT")
    val chords: String?,
    
    @Column(nullable = false)
    val isVerified: Boolean = false,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)