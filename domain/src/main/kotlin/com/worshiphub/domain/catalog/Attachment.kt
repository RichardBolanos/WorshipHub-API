package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Attachment entity for song resources (links, files).
 * 
 * @property id Unique identifier for the attachment
 * @property songId Reference to the song this attachment belongs to
 * @property name Display name for the attachment
 * @property url URL or file path to the resource
 * @property type Type of attachment (link, pdf, audio, etc.)
 * @property createdAt Timestamp when the attachment was added
 */
@Entity
@Table(name = "attachments")
data class Attachment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val songId: UUID,
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    @Column(nullable = false, length = 500)
    val url: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: AttachmentType,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of attachments that can be associated with songs.
 */
enum class AttachmentType {
    YOUTUBE_LINK,
    SPOTIFY_LINK,
    PDF_SHEET,
    AUDIO_FILE,
    OTHER_LINK
}