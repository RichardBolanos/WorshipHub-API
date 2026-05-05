package com.worshiphub.api.communication

import com.worshiphub.api.common.BadRequestException
import com.worshiphub.application.notification.DeviceTokenService
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST controller for managing FCM device tokens.
 *
 * Handles registration and unregistration of device tokens used for push notifications.
 *
 * Validates: Requirements 1.2, 1.5, 1.7
 */
@Tag(name = "Device Tokens", description = "FCM device token registration and management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/devices/token")
class DeviceTokenController(
    private val deviceTokenService: DeviceTokenService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Register FCM device token",
        description = "Registers a Firebase Cloud Messaging token for the authenticated user's device. " +
            "If the token already exists, updates its last-used timestamp instead of creating a duplicate."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Device token registered successfully",
                content = [Content(schema = Schema(implementation = RegisterTokenResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data — missing token or unsupported platform"),
            ApiResponse(responseCode = "401", description = "User not authenticated"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions")
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun registerToken(@Valid @RequestBody request: RegisterTokenRequest): RegisterTokenResponse {
        val userId = securityContext.getCurrentUserId()

        val result = deviceTokenService.registerToken(userId, request.token, request.platform)
        return if (result.isSuccess) {
            RegisterTokenResponse(id = result.getOrThrow())
        } else {
            throw BadRequestException(
                result.exceptionOrNull()?.message ?: "Failed to register device token"
            )
        }
    }

    @Operation(
        summary = "Unregister FCM device token",
        description = "Removes a Firebase Cloud Messaging token for the authenticated user's device, " +
            "typically called on logout to stop receiving push notifications on this device."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Device token unregistered successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request data — missing token"),
            ApiResponse(responseCode = "401", description = "User not authenticated"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions")
        ]
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun unregisterToken(@Valid @RequestBody request: UnregisterTokenRequest) {
        val userId = securityContext.getCurrentUserId()

        val result = deviceTokenService.unregisterToken(userId, request.token)
        if (result.isFailure) {
            throw BadRequestException(
                result.exceptionOrNull()?.message ?: "Failed to unregister device token"
            )
        }
    }
}

// --- DTOs ---

@Schema(description = "Request to register an FCM device token")
data class RegisterTokenRequest(
    @field:NotBlank(message = "Token is required")
    @Schema(
        description = "The FCM device token",
        example = "dGhpcyBpcyBhIHNhbXBsZSBGQ00gdG9rZW4...",
        required = true
    )
    val token: String,

    @field:NotBlank(message = "Platform is required")
    @field:Pattern(
        regexp = "^(ANDROID|IOS|WEB)$",
        message = "Platform must be one of: ANDROID, IOS, WEB"
    )
    @Schema(
        description = "The device platform",
        example = "ANDROID",
        allowableValues = ["ANDROID", "IOS", "WEB"],
        required = true
    )
    val platform: String
)

@Schema(description = "Response after registering an FCM device token")
data class RegisterTokenResponse(
    @Schema(description = "The registered device token's unique identifier")
    val id: UUID
)

@Schema(description = "Request to unregister an FCM device token")
data class UnregisterTokenRequest(
    @field:NotBlank(message = "Token is required")
    @Schema(
        description = "The FCM device token to unregister",
        example = "dGhpcyBpcyBhIHNhbXBsZSBGQ00gdG9rZW4...",
        required = true
    )
    val token: String
)
