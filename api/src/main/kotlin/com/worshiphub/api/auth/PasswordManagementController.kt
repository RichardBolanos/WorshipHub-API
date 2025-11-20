package com.worshiphub.api.auth

import com.worshiphub.api.common.MessageResponse
import com.worshiphub.application.auth.PasswordManagementService
import com.worshiphub.application.auth.SetPasswordResult
import com.worshiphub.application.auth.ChangePasswordResult
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Password Management", description = "Password creation and management operations")
@RestController
@RequestMapping("/api/v1/auth/password")
class PasswordManagementController(
    private val passwordManagementService: PasswordManagementService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Set password for OAuth users",
        description = "Allows users who registered via OAuth to create a password for traditional login",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password set successfully"),
        ApiResponse(responseCode = "400", description = "Password requirements not met or user already has password"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PostMapping("/set")
    @PreAuthorize("isAuthenticated()")
    fun setPassword(@Valid @RequestBody request: SetPasswordRequest): ResponseEntity<MessageResponse> {
        val userId = securityContext.getCurrentUserId()
        
        return when (val result = passwordManagementService.setPassword(userId, request.password)) {
            is SetPasswordResult.Success -> 
                ResponseEntity.ok(MessageResponse("Password set successfully. You can now use traditional login."))
            is SetPasswordResult.InvalidPassword -> 
                ResponseEntity.badRequest().body(MessageResponse("Password requirements not met: ${result.errors.joinToString(", ")}"))
            is SetPasswordResult.PasswordAlreadyExists -> 
                ResponseEntity.badRequest().body(MessageResponse("Password already exists. Use change password instead."))
            is SetPasswordResult.UserNotFound -> 
                ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Change existing password",
        description = "Changes the current password for users who already have one",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password changed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid current password or new password requirements not met"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PutMapping("/change")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): ResponseEntity<MessageResponse> {
        val userId = securityContext.getCurrentUserId()
        
        return when (val result = passwordManagementService.changePassword(userId, request.currentPassword, request.newPassword)) {
            is ChangePasswordResult.Success -> 
                ResponseEntity.ok(MessageResponse("Password changed successfully"))
            is ChangePasswordResult.InvalidCurrentPassword -> 
                ResponseEntity.badRequest().body(MessageResponse("Current password is incorrect"))
            is ChangePasswordResult.InvalidNewPassword -> 
                ResponseEntity.badRequest().body(MessageResponse("New password requirements not met: ${result.errors.joinToString(", ")}"))
            is ChangePasswordResult.NoPasswordSet -> 
                ResponseEntity.badRequest().body(MessageResponse("No password set. Use set password endpoint instead."))
            is ChangePasswordResult.UserNotFound -> 
                ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Check password status",
        description = "Returns whether the authenticated user has a password set",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password status retrieved successfully"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    fun getPasswordStatus(): ResponseEntity<PasswordStatusResponse> {
        val userId = securityContext.getCurrentUserId()
        val hasPassword = passwordManagementService.hasPassword(userId)
        
        return ResponseEntity.ok(PasswordStatusResponse(
            hasPassword = hasPassword,
            canSetPassword = !hasPassword,
            canChangePassword = hasPassword
        ))
    }
}

data class SetPasswordRequest(
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "New password must be at least 8 characters")
    val newPassword: String
)

data class PasswordStatusResponse(
    val hasPassword: Boolean,
    val canSetPassword: Boolean,
    val canChangePassword: Boolean
)