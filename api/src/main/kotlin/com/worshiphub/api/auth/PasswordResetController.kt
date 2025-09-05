package com.worshiphub.api.auth

import com.worshiphub.api.common.MessageResponse
import com.worshiphub.application.auth.PasswordResetResult
import com.worshiphub.application.auth.PasswordResetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Password Reset", description = "Password recovery operations")
@RestController
@RequestMapping("/api/v1/auth/password")
class PasswordResetController(
    private val passwordResetService: PasswordResetService
) {

    @Operation(
        summary = "Request password reset",
        description = "Sends password reset email to the specified email address"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset email sent (or email not found - no disclosure)"),
        ApiResponse(responseCode = "400", description = "Invalid email format")
    ])
    @PostMapping("/forgot")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<MessageResponse> {
        passwordResetService.initiatePasswordReset(request.email)
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(MessageResponse("If the email exists, a password reset link has been sent"))
    }

    @Operation(
        summary = "Validate reset token",
        description = "Validates if a password reset token is valid and not expired"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token is valid"),
        ApiResponse(responseCode = "400", description = "Invalid or expired token")
    ])
    @GetMapping("/reset/{token}/validate")
    fun validateResetToken(
        @Parameter(description = "Password reset token", required = true)
        @PathVariable token: String
    ): ResponseEntity<MessageResponse> {
        return when (val result = passwordResetService.validateResetToken(token)) {
            is PasswordResetResult.Success -> 
                ResponseEntity.ok(MessageResponse("Token is valid"))
            is PasswordResetResult.InvalidToken -> 
                ResponseEntity.badRequest().body(MessageResponse("Invalid reset token"))
            is PasswordResetResult.TokenExpired -> 
                ResponseEntity.badRequest().body(MessageResponse("Reset token has expired"))
            is PasswordResetResult.TokenAlreadyUsed -> 
                ResponseEntity.badRequest().body(MessageResponse("Reset token has already been used"))
            else -> ResponseEntity.badRequest().body(MessageResponse("Invalid token"))
        }
    }

    @Operation(
        summary = "Reset password",
        description = "Resets user password using the provided token and new password"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset successfully"),
        ApiResponse(responseCode = "400", description = "Invalid token or password requirements not met"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PostMapping("/reset")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<MessageResponse> {
        return when (val result = passwordResetService.resetPassword(request.token, request.newPassword)) {
            is PasswordResetResult.Success -> 
                ResponseEntity.ok(MessageResponse("Password reset successfully"))
            is PasswordResetResult.InvalidToken -> 
                ResponseEntity.badRequest().body(MessageResponse("Invalid reset token"))
            is PasswordResetResult.TokenExpired -> 
                ResponseEntity.badRequest().body(MessageResponse("Reset token has expired"))
            is PasswordResetResult.TokenAlreadyUsed -> 
                ResponseEntity.badRequest().body(MessageResponse("Reset token has already been used"))
            is PasswordResetResult.InvalidPassword -> 
                ResponseEntity.badRequest().body(MessageResponse("Password requirements not met: ${result.errors.joinToString(", ")}"))
            is PasswordResetResult.UserNotFound -> 
                ResponseEntity.notFound().build()
        }
    }
}

data class ForgotPasswordRequest(
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)