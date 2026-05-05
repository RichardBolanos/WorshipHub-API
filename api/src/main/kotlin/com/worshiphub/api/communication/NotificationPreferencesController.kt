package com.worshiphub.api.communication

import com.worshiphub.api.common.BadRequestException
import com.worshiphub.application.notification.NotificationPreferencesService
import com.worshiphub.application.notification.UpdatePreferencesCommand
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * REST controller for managing user notification preferences with role-aware support.
 *
 * Provides endpoints to retrieve and update notification preferences for the
 * authenticated user. The GET response includes the set of notification types
 * applicable to the user's current role, so the frontend can display only
 * relevant preference toggles.
 *
 * Validates: Requirements 11.1, 11.2, 11.5, 30.3
 */
@Tag(name = "Notification Preferences", description = "User notification preference management with role-aware filtering")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notifications/preferences")
class NotificationPreferencesController(
    private val notificationPreferencesService: NotificationPreferencesService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Get notification preferences",
        description = "Retrieves the authenticated user's notification preferences, including " +
            "the list of notification types applicable to their current role and the user's effective role. " +
            "The frontend should use 'applicableTypes' to show only relevant preference toggles."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Preferences retrieved successfully",
                content = [Content(schema = Schema(implementation = NotificationPreferencesResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Failed to retrieve preferences"),
            ApiResponse(responseCode = "401", description = "User not authenticated"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions")
        ]
    )
    @GetMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getPreferences(): NotificationPreferencesResponse {
        val userId = securityContext.getCurrentUserId()

        val result = notificationPreferencesService.getPreferences(userId)
        if (result.isFailure) {
            throw BadRequestException(
                result.exceptionOrNull()?.message ?: "Failed to retrieve notification preferences"
            )
        }

        val serviceResponse = result.getOrThrow()
        val visiblePreferences = serviceResponse.getVisiblePreferences()

        return NotificationPreferencesResponse(
            preferences = PreferencesDto.fromVisiblePreferences(visiblePreferences, serviceResponse.preferences),
            applicableTypes = serviceResponse.applicableTypes.map { it.name },
            userRole = serviceResponse.userRole.name
        )
    }

    @Operation(
        summary = "Update notification preferences",
        description = "Updates the authenticated user's notification preferences. " +
            "Only the fields present (non-null) in the request body are updated; " +
            "all other fields retain their current values."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Preferences updated successfully",
                content = [Content(schema = Schema(implementation = NotificationPreferencesResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data or update failed"),
            ApiResponse(responseCode = "401", description = "User not authenticated"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions")
        ]
    )
    @PutMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun updatePreferences(
        @Valid @RequestBody request: UpdateNotificationPreferencesRequest
    ): NotificationPreferencesResponse {
        val userId = securityContext.getCurrentUserId()

        val command = UpdatePreferencesCommand(
            serviceAssignments = request.serviceAssignments,
            chatMessages = request.chatMessages,
            songComments = request.songComments,
            teamChanges = request.teamChanges,
            newSongs = request.newSongs,
            serviceReminders = request.serviceReminders,
            invitationResponses = request.invitationResponses,
            setlistChanges = request.setlistChanges,
            serviceCancellations = request.serviceCancellations,
            recurringServices = request.recurringServices,
            songUpdates = request.songUpdates,
            songDeletions = request.songDeletions,
            songAttachments = request.songAttachments,
            invitationAccepted = request.invitationAccepted,
            availabilityChanges = request.availabilityChanges
        )

        val updateResult = notificationPreferencesService.updatePreferences(userId, command)
        if (updateResult.isFailure) {
            throw BadRequestException(
                updateResult.exceptionOrNull()?.message ?: "Failed to update notification preferences"
            )
        }

        // Re-fetch preferences to return the full role-aware response
        val getResult = notificationPreferencesService.getPreferences(userId)
        if (getResult.isFailure) {
            throw BadRequestException(
                getResult.exceptionOrNull()?.message ?: "Failed to retrieve updated notification preferences"
            )
        }

        val serviceResponse = getResult.getOrThrow()
        val visiblePreferences = serviceResponse.getVisiblePreferences()

        return NotificationPreferencesResponse(
            preferences = PreferencesDto.fromVisiblePreferences(visiblePreferences, serviceResponse.preferences),
            applicableTypes = serviceResponse.applicableTypes.map { it.name },
            userRole = serviceResponse.userRole.name
        )
    }
}

// --- DTOs ---

/**
 * DTO representing the individual preference fields in a frontend-friendly format.
 *
 * Only preferences visible for the user's current role are included with their
 * actual values. Non-applicable preferences are still present but reflect the
 * stored DB value (hidden by the frontend using `applicableTypes`).
 */
@Schema(description = "Individual notification preference settings")
data class PreferencesDto(
    @Schema(description = "Push notifications for service assignments", example = "true")
    val serviceAssignments: Boolean,

    @Schema(description = "Push notifications for chat messages", example = "true")
    val chatMessages: Boolean,

    @Schema(description = "Push notifications for song comments", example = "true")
    val songComments: Boolean,

    @Schema(description = "Push notifications for team changes", example = "true")
    val teamChanges: Boolean,

    @Schema(description = "Push notifications for new songs", example = "true")
    val newSongs: Boolean,

    @Schema(description = "Push notifications for service reminders", example = "true")
    val serviceReminders: Boolean,

    @Schema(description = "Push notifications for invitation responses", example = "true")
    val invitationResponses: Boolean,

    @Schema(description = "Push notifications for setlist changes", example = "true")
    val setlistChanges: Boolean,

    @Schema(description = "Push notifications for service cancellations", example = "true")
    val serviceCancellations: Boolean,

    @Schema(description = "Push notifications for recurring services", example = "true")
    val recurringServices: Boolean,

    @Schema(description = "Push notifications for song updates", example = "true")
    val songUpdates: Boolean,

    @Schema(description = "Push notifications for song deletions", example = "true")
    val songDeletions: Boolean,

    @Schema(description = "Push notifications for song attachments", example = "true")
    val songAttachments: Boolean,

    @Schema(description = "Push notifications for invitation accepted", example = "true")
    val invitationAccepted: Boolean,

    @Schema(description = "Push notifications for availability changes", example = "true")
    val availabilityChanges: Boolean
) {
    companion object {
        /**
         * Creates a [PreferencesDto] from the visible preferences map and the full
         * [NotificationPreference] entity. The visible preferences map contains only
         * the types applicable to the user's role with their enabled/disabled state.
         * For all fields, we use the stored DB values directly.
         */
        fun fromVisiblePreferences(
            visiblePreferences: Map<com.worshiphub.domain.collaboration.NotificationType, Boolean>,
            preferences: com.worshiphub.domain.collaboration.push.NotificationPreference
        ): PreferencesDto = PreferencesDto(
            serviceAssignments = preferences.serviceAssignments,
            chatMessages = preferences.chatMessages,
            songComments = preferences.songComments,
            teamChanges = preferences.teamChanges,
            newSongs = preferences.newSongs,
            serviceReminders = preferences.serviceReminders,
            invitationResponses = preferences.invitationResponses,
            setlistChanges = preferences.setlistChanges,
            serviceCancellations = preferences.serviceCancellations,
            recurringServices = preferences.recurringServices,
            songUpdates = preferences.songUpdates,
            songDeletions = preferences.songDeletions,
            songAttachments = preferences.songAttachments,
            invitationAccepted = preferences.invitationAccepted,
            availabilityChanges = preferences.availabilityChanges
        )
    }
}

/**
 * Response DTO for notification preferences with role-aware information.
 *
 * The `applicableTypes` field indicates which notification types are relevant
 * for the user's current role. The frontend should use this to show only the
 * applicable preference toggles.
 *
 * The `userRole` field indicates the user's effective role (ADMIN, TEAM_LEADER, or MEMBER).
 */
@Schema(description = "Notification preferences response with role-aware filtering information")
data class NotificationPreferencesResponse(
    @Schema(description = "The user's notification preference settings")
    val preferences: PreferencesDto,

    @Schema(
        description = "List of notification types applicable to the user's current role. " +
            "The frontend should only show preference toggles for these types.",
        example = "[\"SERVICE_INVITATION\", \"CHAT_MESSAGE\", \"NEW_COMMENT\", \"NEW_SONG\"]"
    )
    val applicableTypes: List<String>,

    @Schema(
        description = "The user's effective role (highest hierarchy role across all churches/teams)",
        example = "MEMBER",
        allowableValues = ["ADMIN", "TEAM_LEADER", "MEMBER"]
    )
    val userRole: String
)

/**
 * Request DTO for updating notification preferences.
 *
 * All fields are optional (nullable). Only non-null fields will be applied
 * to the user's existing preferences, allowing partial updates.
 */
@Schema(description = "Request to update notification preferences. Only non-null fields are updated.")
data class UpdateNotificationPreferencesRequest(
    @Schema(description = "Enable/disable service assignment notifications", example = "true", nullable = true)
    val serviceAssignments: Boolean? = null,

    @Schema(description = "Enable/disable chat message notifications", example = "true", nullable = true)
    val chatMessages: Boolean? = null,

    @Schema(description = "Enable/disable song comment notifications", example = "true", nullable = true)
    val songComments: Boolean? = null,

    @Schema(description = "Enable/disable team change notifications", example = "true", nullable = true)
    val teamChanges: Boolean? = null,

    @Schema(description = "Enable/disable new song notifications", example = "true", nullable = true)
    val newSongs: Boolean? = null,

    @Schema(description = "Enable/disable service reminder notifications", example = "true", nullable = true)
    val serviceReminders: Boolean? = null,

    @Schema(description = "Enable/disable invitation response notifications", example = "true", nullable = true)
    val invitationResponses: Boolean? = null,

    @Schema(description = "Enable/disable setlist change notifications", example = "true", nullable = true)
    val setlistChanges: Boolean? = null,

    @Schema(description = "Enable/disable service cancellation notifications", example = "true", nullable = true)
    val serviceCancellations: Boolean? = null,

    @Schema(description = "Enable/disable recurring service notifications", example = "true", nullable = true)
    val recurringServices: Boolean? = null,

    @Schema(description = "Enable/disable song update notifications", example = "true", nullable = true)
    val songUpdates: Boolean? = null,

    @Schema(description = "Enable/disable song deletion notifications", example = "true", nullable = true)
    val songDeletions: Boolean? = null,

    @Schema(description = "Enable/disable song attachment notifications", example = "true", nullable = true)
    val songAttachments: Boolean? = null,

    @Schema(description = "Enable/disable invitation accepted notifications", example = "true", nullable = true)
    val invitationAccepted: Boolean? = null,

    @Schema(description = "Enable/disable availability change notifications", example = "true", nullable = true)
    val availabilityChanges: Boolean? = null
)
