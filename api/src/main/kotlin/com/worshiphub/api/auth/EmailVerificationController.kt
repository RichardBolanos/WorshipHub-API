package com.worshiphub.api.auth

import com.worshiphub.api.common.MessageResponse
import com.worshiphub.application.auth.EmailVerificationResult
import com.worshiphub.application.auth.EmailVerificationService
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Email Verification", description = "Email verification operations")
@RestController
@RequestMapping("/api/v1/auth/email")
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Send email verification",
        description = "Sends verification email to the authenticated user"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Verification email sent"),
        ApiResponse(responseCode = "400", description = "Email already verified"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PostMapping("/send-verification")
    @PreAuthorize("isAuthenticated()")
    fun sendEmailVerification(): ResponseEntity<MessageResponse> {
        val userId = securityContext.getCurrentUserId()
        
        return when (val result = emailVerificationService.sendEmailVerification(userId)) {
            is EmailVerificationResult.Success -> 
                ResponseEntity.ok(MessageResponse("Verification email sent successfully"))
            is EmailVerificationResult.AlreadyVerified -> 
                ResponseEntity.badRequest().body(MessageResponse("Email is already verified"))
            is EmailVerificationResult.UserNotFound -> 
                ResponseEntity.notFound().build()
            else -> ResponseEntity.badRequest().body(MessageResponse("Failed to send verification email"))
        }
    }

    @Operation(
        summary = "Verify email address",
        description = "Verifies user email using the token sent via email"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Email verified successfully"),
        ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @GetMapping("/verify/{token}")
    fun verifyEmail(
        @Parameter(description = "Email verification token", required = true)
        @PathVariable token: String
    ): ResponseEntity<MessageResponse> {
        return when (val result = emailVerificationService.verifyEmail(token)) {
            is EmailVerificationResult.Success -> 
                ResponseEntity.ok(MessageResponse("Email verified successfully"))
            is EmailVerificationResult.InvalidToken -> 
                ResponseEntity.badRequest().body(MessageResponse("Invalid verification token"))
            is EmailVerificationResult.TokenExpired -> 
                ResponseEntity.badRequest().body(MessageResponse("Verification token has expired"))
            is EmailVerificationResult.TokenAlreadyUsed -> 
                ResponseEntity.badRequest().body(MessageResponse("Verification token has already been used"))
            is EmailVerificationResult.UserNotFound -> 
                ResponseEntity.notFound().build()
            is EmailVerificationResult.AlreadyVerified -> 
                ResponseEntity.badRequest().body(MessageResponse("Email is already verified"))
        }
    }
}