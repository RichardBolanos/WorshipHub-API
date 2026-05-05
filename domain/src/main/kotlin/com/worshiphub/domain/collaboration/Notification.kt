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
 * @property relatedEntityId Optional ID of the related entity for deep linking (e.g., service ID, song ID, team ID)
 * @property relatedEntityType Optional type of the related entity for deep linking (e.g., "SERVICE", "SONG", "TEAM")
 */
@Entity
@Table(name = "notifications")
data class Notification(
    @Id
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
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "related_entity_id")
    val relatedEntityId: UUID? = null,
    
    @Column(name = "related_entity_type", length = 50)
    val relatedEntityType: String? = null
)

/**
 * Types of notifications.
 */
enum class NotificationType {
    SERVICE_INVITATION,
    NEW_SONG,
    SONG_ADDED,              // When a new song is added to catalog
    NEW_COMMENT,
    TEAM_ASSIGNMENT,
    SERVICE_SCHEDULED,       // When a service is scheduled with team members
    TEAM_MEMBER_ADDED,
    TEAM_MEMBER_REMOVED,
    TEAM_ROLE_CHANGED,
    TEAM_LEADER_CHANGED,
    CHAT_MESSAGE,            // When a chat message is sent in a team
    SETLIST_MODIFIED,        // When a setlist is modified for a future service
    SERVICE_CANCELLED,       // When a service is cancelled
    CHURCH_INVITATION,       // When a user is invited to join a church
    INVITATION_RESPONSE,     // When a member accepts or declines a service assignment
    RECURRING_SERVICE,       // When a recurring service is created, updated, or deleted
    SONG_UPDATED,            // When song details are updated (key, BPM, lyrics, chords)
    SONG_DELETED,            // When a song is deleted from the catalog
    SONG_ATTACHMENT,         // When an attachment is added to a song
    INVITATION_ACCEPTED,     // When a user accepts a church invitation
    AVAILABILITY_CHANGE      // When a member's availability changes
}