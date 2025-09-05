package com.worshiphub.domain.collaboration

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * ChatMessage entity for team chat messages.
 * 
 * @property id Unique identifier for the message
 * @property teamId Reference to the team
 * @property userId Reference to the user who sent the message
 * @property content Message content
 * @property createdAt Timestamp when the message was sent
 */
@Entity
@Table(name = "chat_messages")
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val teamId: UUID,
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)