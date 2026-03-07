package com.worshiphub.domain.collaboration

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * SongComment entity for discussions about song arrangements.
 * 
 * @property id Unique identifier for the comment
 * @property songId Reference to the song
 * @property userId Reference to the user who made the comment
 * @property content Comment content
 * @property createdAt Timestamp when the comment was created
 */
@Entity
@Table(name = "song_comments")
data class SongComment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val songId: UUID,
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)