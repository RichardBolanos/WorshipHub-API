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
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Base64

@Tag(name = "OAuth2 Authentication", description = "Google OAuth2 login operations")
@RestController
@RequestMapping("/api/v1/auth/oauth2")
class OAuth2Controller(
    private val oAuth2AuthenticationService: OAuth2AuthenticationService
) {
    private val objectMapper = ObjectMapper()

    private fun decodeGoogleIdToken(idToken: String): Map<String, Any>? {
        return try {
            val parts = idToken.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val decodedBytes = Base64.getUrlDecoder().decode(payload)
            val payloadJson = String(decodedBytes)
            
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(payloadJson, Map::class.java) as? Map<String, Any>
        } catch (e: Exception) {
            null
        }
    }

    @Operation(
        summary = "Handle Google OAuth2 callback",
        description = "Processes Google OAuth2 authentication and handles pending invitations"
    )
    @GetMapping("/google/callback")
    fun handleGoogleCallback(
        @RequestParam("id_token") idToken: String?
    ): ResponseEntity<OAuth2LoginResponse> {
        return try {
            val token = idToken?.takeIf { it.isNotBlank() }
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Google ID token not provided")
                )

            // Decode the JWT token to extract user information
            val userInfo = decodeGoogleIdToken(token)
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Invalid Google ID token")
                )

            val validEmail = userInfo["email"] as? String
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Valid email not provided by Google")
                )

            val userName = (userInfo["name"] as? String)?.take(100) ?: ""
            val userGivenName = (userInfo["given_name"] as? String)?.take(50) ?: ""
            val userFamilyName = (userInfo["family_name"] as? String)?.take(50) ?: ""

            when (val result = oAuth2AuthenticationService.handleGoogleLogin(validEmail, userName, userGivenName, userFamilyName)) {
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
        @RequestParam email: String?
    ): ResponseEntity<OAuth2LoginResponse> {
        return try {
            // Validate invitation ID format
            if (invitationId.isBlank() || invitationId.length > 36) {
                return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Invalid invitation ID")
                )
            }

            val validEmail = email?.takeIf { it.isNotBlank() && it.contains("@") }
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Valid email not provided")
                )

            when (val result = oAuth2AuthenticationService.acceptInvitationAfterOAuth(invitationId, validEmail)) {
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

