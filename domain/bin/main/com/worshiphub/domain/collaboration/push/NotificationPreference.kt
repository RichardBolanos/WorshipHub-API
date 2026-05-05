package com.worshiphub.domain.collaboration.push

import com.worshiphub.domain.collaboration.NotificationType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Domain entity representing a user's notification preferences.
 *
 * Each user has one set of preferences that controls which types of push
 * notifications they receive. All preference fields default to `true`,
 * meaning new users receive all notification types by default.
 *
 * The in-app notification is always stored regardless of these preferences;
 * only the push delivery is gated by these settings.
 *
 * @property id Unique identifier for the preference record
 * @property userId Reference to the user who owns these preferences (unique)
 * @property serviceAssignments Push for service assignment notifications (SERVICE_INVITATION)
 * @property chatMessages Push for chat message notifications (CHAT_MESSAGE)
 * @property songComments Push for song comment notifications (NEW_COMMENT)
 * @property teamChanges Push for team change notifications (TEAM_ASSIGNMENT, TEAM_MEMBER_*, TEAM_LEADER_CHANGED, TEAM_ROLE_CHANGED, CHURCH_INVITATION)
 * @property newSongs Push for new song notifications (NEW_SONG, SONG_ADDED)
 * @property serviceReminders Push for service reminder/schedule notifications (SERVICE_SCHEDULED, SETLIST_MODIFIED)
 * @property invitationResponses Push for invitation response notifications (INVITATION_RESPONSE)
 * @property setlistChanges Push for setlist modification notifications (SETLIST_MODIFIED)
 * @property serviceCancellations Push for service cancellation notifications (SERVICE_CANCELLED)
 * @property recurringServices Push for recurring service notifications (RECURRING_SERVICE)
 * @property songUpdates Push for song update notifications (SONG_UPDATED)
 * @property songDeletions Push for song deletion notifications (SONG_DELETED)
 * @property songAttachments Push for song attachment notifications (SONG_ATTACHMENT)
 * @property invitationAccepted Push for invitation accepted notifications (INVITATION_ACCEPTED)
 * @property availabilityChanges Push for availability change notifications (AVAILABILITY_CHANGE)
 * @property updatedAt Timestamp of the last preference update
 *
 * Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.6
 */
@Entity
@Table(name = "notification_preferences")
data class NotificationPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val userId: UUID,

    @Column(nullable = false)
    val serviceAssignments: Boolean = true,

    @Column(nullable = false)
    val chatMessages: Boolean = true,

    @Column(nullable = false)
    val songComments: Boolean = true,

    @Column(nullable = false)
    val teamChanges: Boolean = true,

    @Column(nullable = false)
    val newSongs: Boolean = true,

    @Column(nullable = false)
    val serviceReminders: Boolean = true,

    @Column(nullable = false)
    val invitationResponses: Boolean = true,

    @Column(nullable = false)
    val setlistChanges: Boolean = true,

    @Column(nullable = false)
    val serviceCancellations: Boolean = true,

    @Column(nullable = false)
    val recurringServices: Boolean = true,

    @Column(nullable = false)
    val songUpdates: Boolean = true,

    @Column(nullable = false)
    val songDeletions: Boolean = true,

    @Column(nullable = false)
    val songAttachments: Boolean = true,

    @Column(nullable = false)
    val invitationAccepted: Boolean = true,

    @Column(nullable = false)
    val availabilityChanges: Boolean = true,

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * Checks whether push notifications are enabled for the given notification type.
     *
     * Maps each [NotificationType] to its corresponding boolean preference field.
     * Multiple notification types may map to the same preference field when they
     * belong to the same logical category (e.g., all team-related types map to [teamChanges]).
     */
    fun isEnabled(type: NotificationType): Boolean = when (type) {
        NotificationType.SERVICE_INVITATION -> serviceAssignments
        NotificationType.CHAT_MESSAGE -> chatMessages
        NotificationType.NEW_SONG, NotificationType.SONG_ADDED -> newSongs
        NotificationType.NEW_COMMENT -> songComments
        NotificationType.TEAM_ASSIGNMENT,
        NotificationType.TEAM_MEMBER_ADDED,
        NotificationType.TEAM_MEMBER_REMOVED,
        NotificationType.TEAM_ROLE_CHANGED,
        NotificationType.TEAM_LEADER_CHANGED,
        NotificationType.CHURCH_INVITATION -> teamChanges
        NotificationType.SERVICE_SCHEDULED -> serviceReminders
        NotificationType.SETLIST_MODIFIED -> setlistChanges
        NotificationType.SERVICE_CANCELLED -> serviceCancellations
        NotificationType.INVITATION_RESPONSE -> invitationResponses
        NotificationType.RECURRING_SERVICE -> recurringServices
        NotificationType.SONG_UPDATED -> songUpdates
        NotificationType.SONG_DELETED -> songDeletions
        NotificationType.SONG_ATTACHMENT -> songAttachments
        NotificationType.INVITATION_ACCEPTED -> invitationAccepted
        NotificationType.AVAILABILITY_CHANGE -> availabilityChanges
    }

    /**
     * Returns a copy of these preferences with the specified notification types enabled.
     *
     * Used when a user's role changes to one with more permissions (ascending role change),
     * to enable by default the newly available notification types.
     *
     * @param types The set of notification types to enable
     * @return A new [NotificationPreference] with the specified types enabled and [updatedAt] refreshed
     */
    fun enableTypes(types: Set<NotificationType>): NotificationPreference {
        var updated = this
        for (type in types) {
            updated = when (type) {
                NotificationType.SERVICE_INVITATION -> updated.copy(serviceAssignments = true)
                NotificationType.CHAT_MESSAGE -> updated.copy(chatMessages = true)
                NotificationType.NEW_SONG, NotificationType.SONG_ADDED -> updated.copy(newSongs = true)
                NotificationType.NEW_COMMENT -> updated.copy(songComments = true)
                NotificationType.TEAM_ASSIGNMENT,
                NotificationType.TEAM_MEMBER_ADDED,
                NotificationType.TEAM_MEMBER_REMOVED,
                NotificationType.TEAM_ROLE_CHANGED,
                NotificationType.TEAM_LEADER_CHANGED,
                NotificationType.CHURCH_INVITATION -> updated.copy(teamChanges = true)
                NotificationType.SERVICE_SCHEDULED -> updated.copy(serviceReminders = true)
                NotificationType.SETLIST_MODIFIED -> updated.copy(setlistChanges = true)
                NotificationType.SERVICE_CANCELLED -> updated.copy(serviceCancellations = true)
                NotificationType.INVITATION_RESPONSE -> updated.copy(invitationResponses = true)
                NotificationType.RECURRING_SERVICE -> updated.copy(recurringServices = true)
                NotificationType.SONG_UPDATED -> updated.copy(songUpdates = true)
                NotificationType.SONG_DELETED -> updated.copy(songDeletions = true)
                NotificationType.SONG_ATTACHMENT -> updated.copy(songAttachments = true)
                NotificationType.INVITATION_ACCEPTED -> updated.copy(invitationAccepted = true)
                NotificationType.AVAILABILITY_CHANGE -> updated.copy(availabilityChanges = true)
            }
        }
        return updated.copy(updatedAt = LocalDateTime.now())
    }
}
