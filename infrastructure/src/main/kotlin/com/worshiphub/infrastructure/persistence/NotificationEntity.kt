package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

enum class NotificationType {
    SERVICE_INVITATION, TEAM_ASSIGNMENT, NEW_SONG, NEW_COMMENT
}

@Entity
@Table(name = "notifications")
data class NotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,
    
    @Column(nullable = false)
    val isRead: Boolean = false,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)