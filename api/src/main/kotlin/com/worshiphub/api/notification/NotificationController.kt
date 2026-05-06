package com.worshiphub.api.notification

import com.worshiphub.application.notification.NotificationApplicationService
import com.worshiphub.domain.collaboration.NotificationType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

/**
 * REST controller for managing user notifications.
 *
 * The authenticated user is identified by the [User-Id] HTTP header (added
 * client-side by the Flutter [AuthInterceptor]). Notifications are persisted
 * server-side and remain the single source of truth — the client should
 * always read from this controller and not from any local cache exclusively.
 *
 * Push events that produce notifications are emitted by application services
 * (catalog, scheduling, organization, chat). See `PushEvent` sealed class
 * for the full list of types.
 */
@Tag(name = "Notifications", description = "User notification management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationApplicationService: NotificationApplicationService
) {

    @Operation(
        summary = "Get user notifications",
        description = "Retrieves all notifications for the authenticated user, ordered by " +
            "creation date descending. Each notification includes optional `relatedEntityId` " +
            "and `relatedEntityType` fields used by the client for deep linking " +
            "(e.g. tap a SERVICE_INVITATION → navigate to the service detail)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Notifications retrieved successfully",
                content = [Content(array = io.swagger.v3.oas.annotations.media.ArraySchema(
                    schema = Schema(implementation = NotificationResponse::class)
                ))]
            ),
            ApiResponse(responseCode = "400", description = "Failed to retrieve notifications"),
            ApiResponse(responseCode = "401", description = "User not authenticated"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            ApiResponse(responseCode = "404", description = "User not found")
        ]
    )
    @GetMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getUserNotifications(
        @Parameter(
            description = "ID of the user requesting their notifications. " +
                "Added automatically by the Flutter AuthInterceptor.",
            required = true,
            `in` = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
        )
        @RequestHeader("User-Id") userId: UUID
    ): ResponseEntity<List<NotificationResponse>> {
        return try {
            val result = notificationApplicationService.getUserNotifications(userId)
            val notifications = if (result.isSuccess) {
                result.getOrThrow()
            } else {
                return ResponseEntity.badRequest().build()
            }
            val response = notifications.map { notification ->
                NotificationResponse(
                    id = notification.id,
                    userId = notification.userId,
                    title = notification.title,
                    message = notification.message,
                    type = notification.type,
                    isRead = notification.isRead,
                    createdAt = notification.createdAt,
                    relatedEntityId = notification.relatedEntityId,
                    relatedEntityType = notification.relatedEntityType
                )
            }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    @Operation(
        summary = "Mark notification as read",
        description = "Marks a specific notification as read. Idempotent — subsequent calls " +
            "with the same notificationId are safe. Used by the client when the user taps " +
            "a notification or opens the notifications screen."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Notification marked as read"),
            ApiResponse(responseCode = "400", description = "Invalid notification ID or update failed"),
            ApiResponse(responseCode = "401", description = "User not authenticated"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            ApiResponse(responseCode = "404", description = "Notification not found")
        ]
    )
    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun markAsRead(
        @Parameter(description = "Notification ID", required = true) @PathVariable notificationId: UUID
    ): ResponseEntity<Void> {
        return try {
            val result = notificationApplicationService.markAsRead(notificationId)
            if (result.isSuccess) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.badRequest().build()
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
}

/**
 * Response DTO for a single notification.
 *
 * The optional `relatedEntityId` and `relatedEntityType` fields are used by
 * the client for deep linking when the user taps a notification.
 */
@Schema(description = "User notification with optional deep-link metadata")
data class NotificationResponse(
    @Schema(description = "Unique notification identifier", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val id: UUID,

    @Schema(description = "User who receives this notification", example = "456e7890-e89b-12d3-a456-426614174111")
    val userId: UUID,

    @Schema(description = "Notification title", example = "New service assignment")
    val title: String,

    @Schema(description = "Notification body/message", example = "You have been assigned to Sunday Service")
    val message: String,

    @Schema(
        description = "Notification type. Drives the icon, color, and deep-link route on the client.",
        example = "SERVICE_INVITATION"
    )
    val type: NotificationType,

    @Schema(description = "Whether the user has read this notification", example = "false")
    val isRead: Boolean,

    @Schema(description = "Timestamp when the notification was created", example = "2024-01-07T14:30:00")
    val createdAt: LocalDateTime,

    @Schema(
        description = "ID of the related entity for deep linking (service, song, team, etc.). " +
            "Null when the notification has no associated entity.",
        example = "123e4567-e89b-12d3-a456-426614174000",
        nullable = true
    )
    val relatedEntityId: UUID?,

    @Schema(
        description = "Type of the related entity (SERVICE, SONG, TEAM, etc.) used by the client " +
            "to determine which screen to open on tap.",
        example = "SERVICE",
        nullable = true
    )
    val relatedEntityType: String?
)
