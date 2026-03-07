package com.worshiphub.api.notification

import com.worshiphub.application.notification.NotificationApplicationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getUserNotifications(
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): ResponseEntity<List<Map<String, Any>>> {
        return try {
            val result = notificationApplicationService.getUserNotifications(userId)
            val notifications = if (result.isSuccess) {
                result.getOrThrow()
            } else {
                return ResponseEntity.badRequest().build()
            }
            val response = notifications.map { notification ->
                mapOf(
                    "id" to notification.id,
                    "title" to notification.title,
                    "message" to notification.message,
                    "type" to notification.type,
                    "isRead" to notification.isRead,
                    "createdAt" to notification.createdAt
                )
            }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
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