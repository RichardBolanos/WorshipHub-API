package com.worshiphub.api.notification

import com.worshiphub.application.notification.NotificationApplicationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Notifications", description = "User notification management operations")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationApplicationService: NotificationApplicationService
) {
    
    @Operation(
        summary = "Get user notifications",
        description = "Retrieves all notifications for the authenticated user"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @GetMapping
    fun getUserNotifications(
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): List<Map<String, Any>> {
        val notifications = notificationApplicationService.getUserNotifications(userId)
        return notifications.map { notification ->
            mapOf(
                "id" to notification.id,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type,
                "isRead" to notification.isRead,
                "createdAt" to notification.createdAt
            )
        }
    }
    
    @Operation(
        summary = "Mark notification as read",
        description = "Marks a specific notification as read by the user"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Notification marked as read"),
        ApiResponse(responseCode = "404", description = "Notification not found")
    ])
    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAsRead(
        @Parameter(description = "Notification ID", required = true) @PathVariable notificationId: UUID
    ) {
        notificationApplicationService.markAsRead(notificationId)
    }
}