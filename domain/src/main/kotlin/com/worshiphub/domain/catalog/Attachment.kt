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
) {
    
    /**
     * Validates if the URL is accessible.
     */
    fun isValidUrl(): Boolean {
        return try {
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if this is a streaming service link.
     */
    fun isStreamingLink(): Boolean {
        return type in listOf(AttachmentType.YOUTUBE_LINK, AttachmentType.SPOTIFY_LINK)
    }
    
    /**
     * Checks if this is a downloadable resource.
     */
    fun isDownloadable(): Boolean {
        return type in listOf(AttachmentType.PDF_SHEET, AttachmentType.AUDIO_FILE)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attachment) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Attachment(id=$id, name='$name', type=$type)"
    }
}

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