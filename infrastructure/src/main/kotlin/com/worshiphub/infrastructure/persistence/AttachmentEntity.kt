package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

enum class AttachmentType {
    PDF_SHEET, AUDIO_FILE, YOUTUBE_LINK, SPOTIFY_LINK, OTHER_LINK
}

@Entity
@Table(name = "attachments")
data class AttachmentEntity(
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