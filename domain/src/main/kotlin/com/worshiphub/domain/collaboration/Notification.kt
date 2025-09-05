package com.worshiphub.domain.collaboration

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Notification entity for user notifications.
 * 
 * @property id Unique identifier for the notification
 * @property userId Reference to the user receiving the notification
 * @property title Notification title
 * @property message Notification message
 * @property type Type of notification
 * @property isRead Whether the notification has been read
 * @property createdAt Timestamp when the notification was created
 */
@Entity
@Table(name = "notifications")
data class Notification(
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

/**
 * Types of notifications.
 */
enum class NotificationType {
    SERVICE_INVITATION,
    NEW_SONG,
    NEW_COMMENT,
    TEAM_ASSIGNMENT
}