package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "chat_messages")
data class ChatMessageEntity(
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