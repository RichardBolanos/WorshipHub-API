package com.worshiphub.api.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.worshiphub.application.auth.OAuth2AuthenticationService
import com.worshiphub.application.auth.OAuth2LoginResult
import com.worshiphub.application.auth.PendingInvitationDto
import com.worshiphub.security.JwtAuthenticationEntryPoint
import com.worshiphub.security.JwtAuthenticationFilter
import com.worshiphub.security.JwtTokenProvider
import com.worshiphub.security.SecurityContext
import com.worshiphub.security.oauth2.GoogleIdTokenVerifier
import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

/**
 * Slice tests for [OAuth2Controller].
 *
 * The [GoogleIdTokenVerifier] is mocked so we exercise only the controller's
 * orchestration: how it maps verifier outputs (null vs claims with
 * email_verified true/false) and service results (Success / PendingInvitations
 * / NoInvitations) to HTTP responses.
 *
 * Signature/issuer/audience correctness is covered by [GoogleIdTokenVerifierTest].
 */
@WebMvcTest(OAuth2Controller::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OAuth2Controller — Google login orchestration")
class OAuth2ControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Suppress("unused")
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var googleIdTokenVerifier: GoogleIdTokenVerifier

    @MockkBean
    private lateinit var oAuth2AuthenticationService: OAuth2AuthenticationService

    // Required because @WebMvcTest still discovers security beans on the
    // classpath (JwtAuthenticationFilter / EntryPoint are @Component-scanned).
    // We don't exercise them here — addFilters = false bypasses them — but the
    // context still needs to wire their dependencies.
    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var securityContext: SecurityContext

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun stubVerifierReturnsClaims(
        email: String = "user@example.com",
        emailVerified: Boolean = true,
        name: String = "Jane Doe",
        givenName: String = "Jane",
        familyName: String = "Doe"
    ): JWTClaimsSet {
        val claims = JWTClaimsSet.Builder()
            .subject("google-user-1")
            .issuer("https://accounts.google.com")
            .audience("client-android.apps.googleusercontent.com")
            .expirationTime(Date(System.currentTimeMillis() + 3_600_000))
            .claim("email", email)
            .claim("email_verified", emailVerified)
            .claim("name", name)
            .claim("given_name", givenName)
            .claim("family_name", familyName)
            .build()
        every { googleIdTokenVerifier.verify(any()) } returns claims
        return claims
    }

    private fun stubVerifierRejects() {
        every { googleIdTokenVerifier.verify(any()) } returns null
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET /api/v1/auth/oauth2/google/callback?id_token=...
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /google/callback — input validation")
    inner class InputValidation {

        @Test
        fun `returns 400 when id_token query parameter is missing`() {
            mockMvc.perform(get("/api/v1/auth/oauth2/google/callback"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Google ID token not provided"))
        }

        @Test
        fun `returns 400 when id_token is blank`() {
            mockMvc.perform(get("/api/v1/auth/oauth2/google/callback").param("id_token", "   "))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Google ID token not provided"))
        }
    }

    @Nested
    @DisplayName("GET /google/callback — token verification")
    inner class TokenVerification {

        @Test
        fun `returns 401 when verifier rejects the token`() {
            stubVerifierRejects()

            mockMvc.perform(
                get("/api/v1/auth/oauth2/google/callback").param("id_token", "forged.jwt.token")
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Google ID token"))

            verify(exactly = 0) {
                oAuth2AuthenticationService.handleGoogleLogin(any(), any(), any(), any())
            }
        }

        @Test
        fun `returns 401 when Google reports email_verified=false`() {
            stubVerifierReturnsClaims(emailVerified = false)

            mockMvc.perform(
                get("/api/v1/auth/oauth2/google/callback").param("id_token", "valid.but.unverified")
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Google email is not verified"))

            verify(exactly = 0) {
                oAuth2AuthenticationService.handleGoogleLogin(any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("GET /google/callback — successful login flows")
    inner class SuccessfulLogin {

        @Test
        fun `returns 200 with token when service returns Success`() {
            stubVerifierReturnsClaims(email = "alice@example.com")

            every {
                oAuth2AuthenticationService.handleGoogleLogin(
                    "alice@example.com", "Jane Doe", "Jane", "Doe"
                )
            } returns OAuth2LoginResult.Success(
                token = "jwt-token-abc",
                userId = "user-123",
                pendingInvitations = emptyList()
            )

            mockMvc.perform(
                get("/api/v1/auth/oauth2/google/callback").param("id_token", "valid.token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").value("jwt-token-abc"))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.requiresInvitationAcceptance").value(false))
        }

        @Test
        fun `returns 200 with pending invitations when user has invitations`() {
            stubVerifierReturnsClaims(email = "bob@example.com")

            val invitation = PendingInvitationDto(
                id = "inv-1",
                churchName = "First Baptist",
                role = "MUSICIAN",
                invitedBy = "Pastor Joe",
                expiresAt = "2026-12-31T23:59:59"
            )
            every {
                oAuth2AuthenticationService.handleGoogleLogin(
                    "bob@example.com", "Jane Doe", "Jane", "Doe"
                )
            } returns OAuth2LoginResult.PendingInvitations(listOf(invitation))

            mockMvc.perform(
                get("/api/v1/auth/oauth2/google/callback").param("id_token", "valid.token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("You have pending invitations"))
                .andExpect(jsonPath("$.requiresInvitationAcceptance").value(true))
                .andExpect(jsonPath("$.pendingInvitations[0].id").value("inv-1"))
                .andExpect(jsonPath("$.pendingInvitations[0].churchName").value("First Baptist"))
                .andExpect(jsonPath("$.pendingInvitations[0].role").value("MUSICIAN"))
        }

        @Test
        fun `returns 200 with success=false when no invitations and user is not registered`() {
            stubVerifierReturnsClaims(email = "stranger@example.com")
            every {
                oAuth2AuthenticationService.handleGoogleLogin(any(), any(), any(), any())
            } returns OAuth2LoginResult.NoInvitations

            mockMvc.perform(
                get("/api/v1/auth/oauth2/google/callback").param("id_token", "valid.token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                    jsonPath("$.message").value(
                        "No invitations found for this email. Please contact your church administrator."
                    )
                )
        }
    }

    @Nested
    @DisplayName("GET /google/callback — name field truncation (defense against malicious lengths)")
    inner class NameTruncation {

        @Test
        fun `truncates name to 100 chars and given_name family_name to 50 chars`() {
            val longName = "A".repeat(500)
            val longGiven = "B".repeat(500)
            val longFamily = "C".repeat(500)
            stubVerifierReturnsClaims(
                email = "long@example.com",
                name = longName,
                givenName = longGiven,
                familyName = longFamily
            )
            every {
                oAuth2AuthenticationService.handleGoogleLogin(any(), any(), any(), any())
            } returns OAuth2LoginResult.NoInvitations

            mockMvc.perform(
                get("/api/v1/auth/oauth2/google/callback").param("id_token", "valid.token")
            )
                .andExpect(status().isOk)

            verify(exactly = 1) {
                oAuth2AuthenticationService.handleGoogleLogin(
                    "long@example.com",
                    "A".repeat(100),
                    "B".repeat(50),
                    "C".repeat(50)
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/oauth2/accept-invitation/{invitationId}
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /accept-invitation — input validation")
    inner class AcceptInvitationValidation {

        @Test
        fun `returns 400 when email is missing`() {
            mockMvc.perform(post("/api/v1/auth/oauth2/accept-invitation/some-id"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Valid email not provided"))
        }

        @Test
        fun `returns 400 when email is blank`() {
            mockMvc.perform(
                post("/api/v1/auth/oauth2/accept-invitation/some-id").param("email", "")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `returns 400 when email has no @ symbol`() {
            mockMvc.perform(
                post("/api/v1/auth/oauth2/accept-invitation/some-id").param("email", "not-an-email")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `returns 400 when invitationId is over 36 chars (defense against long path injection)`() {
            val longId = "x".repeat(40)
            mockMvc.perform(
                post("/api/v1/auth/oauth2/accept-invitation/$longId")
                    .param("email", "user@example.com")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Invalid invitation ID"))
        }
    }

    @Nested
    @DisplayName("POST /accept-invitation — happy path")
    inner class AcceptInvitationSuccess {

        @Test
        fun `returns 200 with token when service returns Success`() {
            every {
                oAuth2AuthenticationService.acceptInvitationAfterOAuth(
                    "11111111-1111-1111-1111-111111111111",
                    "user@example.com"
                )
            } returns OAuth2LoginResult.Success(
                token = "fresh-jwt",
                userId = "newly-created-user-id",
                pendingInvitations = emptyList()
            )

            mockMvc.perform(
                post("/api/v1/auth/oauth2/accept-invitation/11111111-1111-1111-1111-111111111111")
                    .param("email", "user@example.com")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invitation accepted successfully"))
                .andExpect(jsonPath("$.token").value("fresh-jwt"))
                .andExpect(jsonPath("$.userId").value("newly-created-user-id"))
        }

        @Test
        fun `returns 400 when service returns NoInvitations (invalid or already used)`() {
            every {
                oAuth2AuthenticationService.acceptInvitationAfterOAuth(any(), any())
            } returns OAuth2LoginResult.NoInvitations

            mockMvc.perform(
                post("/api/v1/auth/oauth2/accept-invitation/11111111-1111-1111-1111-111111111111")
                    .param("email", "user@example.com")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to accept invitation"))
        }
    }
}
