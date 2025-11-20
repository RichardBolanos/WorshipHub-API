package com.worshiphub.api.auth

import com.worshiphub.application.auth.OAuth2AuthenticationService
import com.worshiphub.application.auth.OAuth2LoginResult
import com.worshiphub.application.auth.PendingInvitationDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.*

@Tag(name = "OAuth2 Authentication", description = "Google OAuth2 login operations")
@RestController
@RequestMapping("/api/v1/auth/oauth2")
class OAuth2Controller(
    private val oAuth2AuthenticationService: OAuth2AuthenticationService
) {

    @Operation(
        summary = "Handle Google OAuth2 callback",
        description = "Processes Google OAuth2 authentication and handles pending invitations"
    )
    @GetMapping("/google/callback")
    fun handleGoogleCallback(
        @AuthenticationPrincipal oauth2User: OAuth2User?
    ): ResponseEntity<OAuth2LoginResponse> {
        return try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body(
                    OAuth2LoginResponse(success = false, message = "OAuth2 user not authenticated")
                )
            }

            val email = oauth2User.getAttribute<String>("email")
                ?.takeIf { it.isNotBlank() && it.contains("@") }
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Valid email not provided by Google")
                )

            val name = oauth2User.getAttribute<String>("name")?.take(100) ?: ""
            val givenName = oauth2User.getAttribute<String>("given_name")?.take(50) ?: ""
            val familyName = oauth2User.getAttribute<String>("family_name")?.take(50) ?: ""

            when (val result = oAuth2AuthenticationService.handleGoogleLogin(email, name, givenName, familyName)) {
                is OAuth2LoginResult.Success -> ResponseEntity.ok(
                    OAuth2LoginResponse(
                        success = true,
                        message = "Login successful",
                        token = result.token,
                        userId = result.userId,
                        pendingInvitations = result.pendingInvitations
                    )
                )
                is OAuth2LoginResult.PendingInvitations -> ResponseEntity.ok(
                    OAuth2LoginResponse(
                        success = true,
                        message = "You have pending invitations",
                        pendingInvitations = result.invitations,
                        requiresInvitationAcceptance = true
                    )
                )
                is OAuth2LoginResult.NoInvitations -> ResponseEntity.ok(
                    OAuth2LoginResponse(
                        success = false,
                        message = "No invitations found for this email. Please contact your church administrator."
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OAuth2LoginResponse(
                    success = false, 
                    message = "Authentication failed: ${e.message?.take(100) ?: "Unknown error"}"
                )
            )
        }
    }

    @Operation(
        summary = "Accept invitation after OAuth2 login",
        description = "Accepts a pending invitation and completes user registration"
    )
    @PostMapping("/accept-invitation/{invitationId}")
    fun acceptInvitation(
        @PathVariable invitationId: String,
        @AuthenticationPrincipal oauth2User: OAuth2User?
    ): ResponseEntity<OAuth2LoginResponse> {
        return try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body(
                    OAuth2LoginResponse(success = false, message = "OAuth2 user not authenticated")
                )
            }

            // Validate invitation ID format
            if (invitationId.isBlank() || invitationId.length > 36) {
                return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Invalid invitation ID")
                )
            }

            val email = oauth2User.getAttribute<String>("email")
                ?.takeIf { it.isNotBlank() && it.contains("@") }
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Valid email not provided")
                )

            when (val result = oAuth2AuthenticationService.acceptInvitationAfterOAuth(invitationId, email)) {
                is OAuth2LoginResult.Success -> ResponseEntity.ok(
                    OAuth2LoginResponse(
                        success = true,
                        message = "Invitation accepted successfully",
                        token = result.token,
                        userId = result.userId
                    )
                )
                else -> ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Failed to accept invitation")
                )
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OAuth2LoginResponse(
                    success = false, 
                    message = "Invitation acceptance failed: ${e.message?.take(100) ?: "Unknown error"}"
                )
            )
        }
    }
}

data class OAuth2LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val userId: String? = null,
    val pendingInvitations: List<PendingInvitationDto>? = null,
    val requiresInvitationAcceptance: Boolean = false
)

