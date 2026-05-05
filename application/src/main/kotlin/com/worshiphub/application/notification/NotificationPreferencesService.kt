package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.NotificationPreference
import com.worshiphub.domain.collaboration.push.RoleNotificationFilter
import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for managing user notification preferences with role-aware support.
 *
 * Provides methods to retrieve preferences filtered by the user's effective role,
 * update individual preference fields, and handle role changes by enabling newly
 * available notification types by default.
 *
 * Validates: Requirements 11.1, 11.2, 11.4, 11.5, 11.6, 11.7
 */
@Service
open class NotificationPreferencesService(
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
    private val userRoleResolver: UserRoleResolver
) {

    private val logger = LoggerFactory.getLogger(NotificationPreferencesService::class.java)

    /**
     * Retrieves the notification preferences for a user, including the set of
     * notification types applicable to their effective role.
     *
     * The response includes:
     * - The full [NotificationPreference] entity (all fields preserved in DB)
     * - The [Set] of [NotificationType] applicable to the user's current role
     * - The user's effective [UserRole]
     *
     * The frontend uses [NotificationPreferenceResponse.applicableTypes] to show
     * only the relevant preference toggles for the user's role.
     *
     * @param userId The ID of the user
     * @return [Result] containing [NotificationPreferenceResponse] on success
     *
     * Validates: Requirements 11.1, 11.5
     */
    @Transactional(readOnly = true)
    open fun getPreferences(userId: UUID): Result<NotificationPreferenceResponse> {
        return try {
            val prefs = notificationPreferenceRepository.findByUserIdOrDefault(userId)
            val effectiveRole = userRoleResolver.resolveEffectiveRole(userId)
            val applicableTypes = RoleNotificationFilter.getApplicableTypes(effectiveRole)
            Result.success(NotificationPreferenceResponse(prefs, applicableTypes, effectiveRole))
        } catch (e: Exception) {
            logger.error("Failed to get notification preferences for user {}: {}", userId, e.message, e)
            Result.failure(RuntimeException("Failed to get notification preferences", e))
        }
    }

    /**
     * Updates the notification preferences for a user using partial update semantics.
     *
     * Only the fields present (non-null) in the [UpdatePreferencesCommand] are updated;
     * all other fields retain their current values.
     *
     * @param userId The ID of the user
     * @param command The partial update command with optional fields for each preference type
     * @return [Result] containing the updated [NotificationPreference] on success
     *
     * Validates: Requirements 11.1, 11.2
     */
    @Transactional
    open fun updatePreferences(userId: UUID, command: UpdatePreferencesCommand): Result<NotificationPreference> {
        return try {
            val current = notificationPreferenceRepository.findByUserIdOrDefault(userId)
            val updated = current.copy(
                serviceAssignments = command.serviceAssignments ?: current.serviceAssignments,
                chatMessages = command.chatMessages ?: current.chatMessages,
                songComments = command.songComments ?: current.songComments,
                teamChanges = command.teamChanges ?: current.teamChanges,
                newSongs = command.newSongs ?: current.newSongs,
                serviceReminders = command.serviceReminders ?: current.serviceReminders,
                invitationResponses = command.invitationResponses ?: current.invitationResponses,
                setlistChanges = command.setlistChanges ?: current.setlistChanges,
                serviceCancellations = command.serviceCancellations ?: current.serviceCancellations,
                recurringServices = command.recurringServices ?: current.recurringServices,
                songUpdates = command.songUpdates ?: current.songUpdates,
                songDeletions = command.songDeletions ?: current.songDeletions,
                songAttachments = command.songAttachments ?: current.songAttachments,
                invitationAccepted = command.invitationAccepted ?: current.invitationAccepted,
                availabilityChanges = command.availabilityChanges ?: current.availabilityChanges,
                updatedAt = java.time.LocalDateTime.now()
            )
            val saved = notificationPreferenceRepository.save(updated)
            logger.info("Updated notification preferences for user {}", userId)
            Result.success(saved)
        } catch (e: Exception) {
            logger.error("Failed to update notification preferences for user {}: {}", userId, e.message, e)
            Result.failure(RuntimeException("Failed to update notification preferences", e))
        }
    }

    /**
     * Handles a role change for a user by enabling newly available notification types
     * by default.
     *
     * When a user's role changes (e.g., MEMBER → TEAM_LEADER), the notification types
     * that become newly available for the new role are automatically enabled. Preferences
     * for types that are no longer applicable are preserved in the database but hidden
     * in the frontend.
     *
     * The method resolves the user's current (previous) role to calculate which types
     * are newly available, then enables those types using [NotificationPreference.enableTypes].
     *
     * @param userId The ID of the user whose role changed
     * @param newRole The user's new effective [UserRole]
     *
     * Validates: Requirements 11.6, 11.7
     */
    @Transactional
    open fun onRoleChanged(userId: UUID, newRole: UserRole) {
        try {
            val prefs = notificationPreferenceRepository.findByUserIdOrDefault(userId)

            // Resolve the user's current (previous) role to determine previously applicable types.
            // At this point the user's role in the organization tables may already reflect the new role,
            // so we compute the previous applicable types from the current preferences context.
            val previousRole = userRoleResolver.resolveEffectiveRole(userId)
            val previousApplicable = RoleNotificationFilter.getApplicableTypes(previousRole)
            val newApplicable = RoleNotificationFilter.getApplicableTypes(newRole)

            // Calculate types that are newly available with the new role
            val newlyAvailable = newApplicable - previousApplicable

            if (newlyAvailable.isNotEmpty()) {
                val updatedPrefs = prefs.enableTypes(newlyAvailable)
                notificationPreferenceRepository.save(updatedPrefs)
                logger.info(
                    "Role changed for user {} from {} to {}. Enabled {} newly available notification types: {}",
                    userId, previousRole, newRole, newlyAvailable.size, newlyAvailable
                )
            } else {
                logger.debug(
                    "Role changed for user {} from {} to {}. No new notification types to enable.",
                    userId, previousRole, newRole
                )
            }
            // Preferences for types no longer applicable are preserved in DB (Req 11.7)
        } catch (e: Exception) {
            logger.error(
                "Failed to handle role change for user {} to role {}: {}",
                userId, newRole, e.message, e
            )
        }
    }
}

/**
 * Command for partial update of notification preferences.
 *
 * Each field is optional (nullable). Only non-null fields will be applied
 * to the user's existing preferences, allowing partial updates.
 *
 * @property serviceAssignments Enable/disable service assignment notifications
 * @property chatMessages Enable/disable chat message notifications
 * @property songComments Enable/disable song comment notifications
 * @property teamChanges Enable/disable team change notifications
 * @property newSongs Enable/disable new song notifications
 * @property serviceReminders Enable/disable service reminder notifications
 * @property invitationResponses Enable/disable invitation response notifications
 * @property setlistChanges Enable/disable setlist change notifications
 * @property serviceCancellations Enable/disable service cancellation notifications
 * @property recurringServices Enable/disable recurring service notifications
 * @property songUpdates Enable/disable song update notifications
 * @property songDeletions Enable/disable song deletion notifications
 * @property songAttachments Enable/disable song attachment notifications
 * @property invitationAccepted Enable/disable invitation accepted notifications
 * @property availabilityChanges Enable/disable availability change notifications
 */
data class UpdatePreferencesCommand(
    val serviceAssignments: Boolean? = null,
    val chatMessages: Boolean? = null,
    val songComments: Boolean? = null,
    val teamChanges: Boolean? = null,
    val newSongs: Boolean? = null,
    val serviceReminders: Boolean? = null,
    val invitationResponses: Boolean? = null,
    val setlistChanges: Boolean? = null,
    val serviceCancellations: Boolean? = null,
    val recurringServices: Boolean? = null,
    val songUpdates: Boolean? = null,
    val songDeletions: Boolean? = null,
    val songAttachments: Boolean? = null,
    val invitationAccepted: Boolean? = null,
    val availabilityChanges: Boolean? = null
)

/**
 * Response DTO that includes the user's notification preferences, the set of
 * notification types applicable to their role, and the user's effective role.
 *
 * The frontend uses [applicableTypes] to determine which preference toggles
 * to display, and [userRole] to show the user's current role context.
 *
 * @property preferences The full [NotificationPreference] entity
 * @property applicableTypes The set of [NotificationType] applicable to the user's role
 * @property userRole The user's effective [UserRole]
 */
data class NotificationPreferenceResponse(
    val preferences: NotificationPreference,
    val applicableTypes: Set<NotificationType>,
    val userRole: UserRole
) {
    /**
     * Returns only the preferences visible for the user's current role.
     *
     * Maps each applicable [NotificationType] to its enabled/disabled state
     * from the user's preferences. Types not applicable to the role are excluded.
     *
     * @return A map of applicable notification types to their enabled state
     */
    fun getVisiblePreferences(): Map<NotificationType, Boolean> =
        applicableTypes.associateWith { type -> preferences.isEnabled(type) }
}
