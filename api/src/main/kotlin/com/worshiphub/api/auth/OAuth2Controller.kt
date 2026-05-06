package com.worshiphub.api.auth

import com.worshiphub.application.auth.OAuth2AuthenticationService
import com.worshiphub.application.auth.OAuth2LoginResult
import com.worshiphub.application.auth.PendingInvitationDto
import com.worshiphub.security.oauth2.GoogleIdTokenVerifier
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "OAuth2 Authentication", description = "Google OAuth2 login operations")
@RestController
@RequestMapping("/api/v1/auth/oauth2")
class OAuth2Controller(
    private val oAuth2AuthenticationService: OAuth2AuthenticationService,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier
) {

    @Operation(
        summary = "Handle Google OAuth2 callback",
        description = "Verifies the Google ID token (signature + audience) and processes the login. " +
            "Handles pending invitations if the user has any."
    )
    @SecurityRequirements // No security required — public endpoint
    @GetMapping("/google/callback")
    fun handleGoogleCallback(
        @RequestParam("id_token") idToken: String?
    ): ResponseEntity<OAuth2LoginResponse> {
        return try {
            val token = idToken?.takeIf { it.isNotBlank() }
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Google ID token not provided")
                )

            // SECURITY: verify the signature against Google's JWKS, validate `iss`, `aud`, `exp`.
            // Returns null on any failure (invalid signature, wrong audience, expired, etc.).
            val claims = googleIdTokenVerifier.verify(token)
                ?: return ResponseEntity.status(401).body(
                    OAuth2LoginResponse(success = false, message = "Invalid Google ID token")
                )

            val validEmail = claims.getStringClaim("email")
                ?: return ResponseEntity.badRequest().body(
                    OAuth2LoginResponse(success = false, message = "Email claim missing in Google ID token")
                )

            // Optional: ensure Google has actually verified this email.
            // This claim is `true` for normal Google-account flows and `false` for some federated logins.
            val emailVerified = claims.getBooleanClaim("email_verified") ?: false
            if (!emailVerified) {
                return ResponseEntity.status(401).body(
                    OAuth2LoginResponse(success = false, message = "Google email is not verified")
                )
            }

            val userName = claims.getStringClaim("name")?.take(100) ?: ""
            val userGivenName = claims.getStringClaim("given_name")?.take(50) ?: ""
            val userFamilyName = claims.getStringClaim("family_name")?.take(50) ?: ""

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
    @SecurityRequirements // No security required — public endpoint
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
