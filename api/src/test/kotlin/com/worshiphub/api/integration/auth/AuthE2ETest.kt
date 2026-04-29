package com.worshiphub.api.integration.auth

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
import com.worshiphub.domain.auth.EmailVerificationToken
import com.worshiphub.domain.auth.InvitationToken
import com.worshiphub.domain.auth.PasswordResetToken
import com.worshiphub.domain.auth.repository.EmailVerificationTokenRepository
import com.worshiphub.domain.auth.repository.InvitationTokenRepository
import com.worshiphub.domain.auth.repository.PasswordResetTokenRepository
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.*

/**
 * E2E integration tests for the Auth bounded context.
 * Covers Requirements 1-5: Church registration, authentication, email verification,
 * password management, and invitations.
 */
class AuthE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository

    @Autowired
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    lateinit var invitationTokenRepository: InvitationTokenRepository

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 1: Church Registration
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 1 - Church Registration")
    inner class ChurchRegistration {

        // Validates: Requirement 1.1
        @Test
        fun `should register church with valid data and return 201 with churchId and adminUserId`() {
            val request = mapOf(
                "churchName" to TestConstants.CHURCH_NAME,
                "churchAddress" to TestConstants.CHURCH_ADDRESS,
                "churchEmail" to TestConstants.CHURCH_EMAIL,
                "adminEmail" to "newadmin@testchurch.com",
                "adminFirstName" to TestConstants.ADMIN_FIRST_NAME,
                "adminLastName" to TestConstants.ADMIN_LAST_NAME,
                "adminPassword" to TestConstants.VALID_PASSWORD
            )

            mockMvc.perform(
                post("/api/v1/auth/church/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.churchId").exists())
                .andExpect(jsonPath("$.adminUserId").exists())
                .andExpect(jsonPath("$.message").exists())
        }

        // Validates: Requirement 1.2
        @Test
        fun `should return 409 when registering church with duplicate admin email`() {
            // First registration
            testData.registerChurch(adminEmail = "duplicate@testchurch.com")

            // Second registration with same admin email
            val request = mapOf(
                "churchName" to "Another Church",
                "churchAddress" to "Another Address",
                "churchEmail" to "another@church.com",
                "adminEmail" to "duplicate@testchurch.com",
                "adminFirstName" to "Another",
                "adminLastName" to "Admin",
                "adminPassword" to TestConstants.VALID_PASSWORD
            )

            mockMvc.perform(
                post("/api/v1/auth/church/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
        }

        // Validates: Requirement 1.3
        @Test
        fun `should return 400 when registering church with short password`() {
            val request = mapOf(
                "churchName" to TestConstants.CHURCH_NAME,
                "churchAddress" to TestConstants.CHURCH_ADDRESS,
                "churchEmail" to TestConstants.CHURCH_EMAIL,
                "adminEmail" to "shortpw@testchurch.com",
                "adminFirstName" to TestConstants.ADMIN_FIRST_NAME,
                "adminLastName" to TestConstants.ADMIN_LAST_NAME,
                "adminPassword" to TestConstants.WEAK_PASSWORD
            )

            mockMvc.perform(
                post("/api/v1/auth/church/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        // Validates: Business Rule - Password must contain special character
        @Test
        fun `should return 400 when registering with password lacking special character`() {
            val request = mapOf(
                "churchName" to TestConstants.CHURCH_NAME,
                "churchAddress" to TestConstants.CHURCH_ADDRESS,
                "churchEmail" to TestConstants.CHURCH_EMAIL,
                "adminEmail" to "nospecialchar@testchurch.com",
                "adminFirstName" to TestConstants.ADMIN_FIRST_NAME,
                "adminLastName" to TestConstants.ADMIN_LAST_NAME,
                "adminPassword" to "SecurePass123"
            )

            mockMvc.perform(
                post("/api/v1/auth/church/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        // Validates: Requirement 1.4
        @Test
        fun `should return 400 when registering church with missing required fields`() {
            val request = mapOf(
                "churchName" to "",
                "churchAddress" to "",
                "churchEmail" to "",
                "adminEmail" to "",
                "adminFirstName" to "",
                "adminLastName" to "",
                "adminPassword" to ""
            )

            mockMvc.perform(
                post("/api/v1/auth/church/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        // Validates: Requirement 1.5
        @Test
        fun `should retrieve registered church via GET endpoint`() {
            val registration = testData.registerChurch(adminEmail = "retrieve@testchurch.com")

            mockMvc.perform(
                get("/api/v1/churches/${registration.churchId}")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(registration.churchId.toString()))
                .andExpect(jsonPath("$.name").value(TestConstants.CHURCH_NAME))
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 2: Authentication & Session
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 2 - Authentication & Session")
    inner class AuthenticationAndSession {

        // Validates: Requirement 2.1
        @Test
        fun `should login with valid credentials and return 200 with JWT token and user info`() {
            val email = "login-valid@testchurch.com"
            val password = TestConstants.VALID_PASSWORD

            // Register church (creates admin user with isActive=true, isEmailVerified=true via ChurchRegistrationService)
            val registration = testData.registerChurch(adminEmail = email, adminPassword = password)

            // Manually verify email and activate user since church registration may not auto-verify
            val user = userRepository.findByEmail(email)
            if (user != null && (!user.isEmailVerified || !user.isActive)) {
                userRepository.save(user.copy(isEmailVerified = true, isActive = true))
            }

            val loginRequest = mapOf(
                "email" to email,
                "password" to password
            )

            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.firstName").value(TestConstants.ADMIN_FIRST_NAME))
                .andExpect(jsonPath("$.user.lastName").value(TestConstants.ADMIN_LAST_NAME))
                .andExpect(jsonPath("$.user.role").exists())
                .andExpect(jsonPath("$.user.churchId").value(registration.churchId.toString()))
        }

        // Validates: Requirement 2.2
        @Test
        fun `should return 401 with INVALID_CREDENTIALS when login with wrong password`() {
            val email = "login-invalid@testchurch.com"
            testData.registerChurch(adminEmail = email)

            val loginRequest = mapOf(
                "email" to email,
                "password" to "WrongPassword123!"
            )

            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
        }

        // Validates: Requirement 2.3
        @Test
        fun `should return 403 with EMAIL_NOT_VERIFIED when login with unverified email`() {
            val email = "unverified@testchurch.com"
            val password = TestConstants.VALID_PASSWORD

            // Register church first to get a churchId
            val registration = testData.registerChurch(adminEmail = "setup-unverified@testchurch.com")

            // Create a user with unverified email directly
            val user = User(
                email = email,
                firstName = "Unverified",
                lastName = "User",
                passwordHash = passwordEncoder.encode(password),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = false
            )
            userRepository.save(user)

            val loginRequest = mapOf(
                "email" to email,
                "password" to password
            )

            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_VERIFIED"))
        }

        // Validates: Requirement 2.4
        @Test
        fun `should return 403 with ACCOUNT_INACTIVE when login with inactive account`() {
            val email = "inactive@testchurch.com"
            val password = TestConstants.VALID_PASSWORD

            val registration = testData.registerChurch(adminEmail = "setup-inactive@testchurch.com")

            // Create a user with inactive account but verified email
            val user = User(
                email = email,
                firstName = "Inactive",
                lastName = "User",
                passwordHash = passwordEncoder.encode(password),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = false,
                isEmailVerified = true
            )
            userRepository.save(user)

            val loginRequest = mapOf(
                "email" to email,
                "password" to password
            )

            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error").value("ACCOUNT_INACTIVE"))
        }

        // Validates: Requirement 2.5
        @Test
        fun `should logout with Bearer token and return 200`() {
            mockMvc.perform(
                post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer some-jwt-token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
        }

        // Validates: Requirement 2.6
        @Test
        fun `should register user with valid data and return 201 with userId`() {
            val registration = testData.registerChurch(adminEmail = "setup-register@testchurch.com")

            val registerRequest = mapOf(
                "firstName" to "New",
                "lastName" to "User",
                "email" to "newuser@testchurch.com",
                "password" to TestConstants.VALID_PASSWORD,
                "churchId" to registration.churchId.toString()
            )

            mockMvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.userId").exists())
        }

        // Validates: Requirement 2.7
        @Test
        fun `should return 409 when registering user with duplicate email`() {
            val email = "dup-user@testchurch.com"
            val registration = testData.registerChurch(adminEmail = "setup-dup@testchurch.com")

            // First registration
            val registerRequest = mapOf(
                "firstName" to "First",
                "lastName" to "User",
                "email" to email,
                "password" to TestConstants.VALID_PASSWORD,
                "churchId" to registration.churchId.toString()
            )

            mockMvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isCreated)

            // Second registration with same email
            mockMvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isConflict)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 3: Email Verification
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 3 - Email Verification")
    inner class EmailVerification {

        // Validates: Requirement 3.1
        @Test
        fun `should send email verification and return 200`() {
            val registration = testData.registerChurch(adminEmail = "send-verify@testchurch.com")

            // Mark user as not verified so we can request verification
            val user = userRepository.findByEmail("send-verify@testchurch.com")!!
            userRepository.save(user.copy(isEmailVerified = false))

            mockMvc.perform(
                post("/api/v1/auth/email/send-verification")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Verification email sent successfully"))
        }

        // Validates: Requirement 3.2
        @Test
        fun `should verify email with valid token and return 200 with HTML success`() {
            val registration = testData.registerChurch(adminEmail = "verify-valid@testchurch.com")

            // Create a valid email verification token directly
            val token = EmailVerificationToken(
                userId = registration.adminUserId,
                token = "valid-verification-token-${UUID.randomUUID()}",
                email = "verify-valid@testchurch.com",
                expiresAt = LocalDateTime.now().plusHours(24)
            )
            emailVerificationTokenRepository.save(token)

            mockMvc.perform(
                get("/api/v1/auth/email/verify/${token.token}")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("verificado")))
        }

        // Validates: Requirement 3.3
        @Test
        fun `should return 200 with HTML error for expired verification token`() {
            val registration = testData.registerChurch(adminEmail = "verify-expired@testchurch.com")

            // Create an expired token
            val token = EmailVerificationToken(
                userId = registration.adminUserId,
                token = "expired-verification-token-${UUID.randomUUID()}",
                email = "verify-expired@testchurch.com",
                expiresAt = LocalDateTime.now().minusHours(1) // Already expired
            )
            emailVerificationTokenRepository.save(token)

            mockMvc.perform(
                get("/api/v1/auth/email/verify/${token.token}")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("expirado")))
        }

        // Validates: Requirement 3.4
        @Test
        fun `should return 200 with HTML indicating already-used verification token`() {
            val registration = testData.registerChurch(adminEmail = "verify-used@testchurch.com")

            // Create a used token
            val token = EmailVerificationToken(
                userId = registration.adminUserId,
                token = "used-verification-token-${UUID.randomUUID()}",
                email = "verify-used@testchurch.com",
                expiresAt = LocalDateTime.now().plusHours(24),
                isUsed = true
            )
            emailVerificationTokenRepository.save(token)

            mockMvc.perform(
                get("/api/v1/auth/email/verify/${token.token}")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("utilizado")))
        }

        // Validates: Requirement 3.5
        @Test
        fun `should resend email verification and return 200`() {
            val email = "resend-verify@testchurch.com"
            testData.registerChurch(adminEmail = email)

            // Mark user as not verified
            val user = userRepository.findByEmail(email)!!
            userRepository.save(user.copy(isEmailVerified = false))

            val request = mapOf("email" to email)

            mockMvc.perform(
                post("/api/v1/auth/email/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Verification email sent successfully"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 4: Password Management
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 4 - Password Management")
    inner class PasswordManagement {

        // Validates: Requirement 4.1
        @Test
        fun `should return 200 with generic message for forgot password`() {
            val request = mapOf("email" to "anyone@testchurch.com")

            mockMvc.perform(
                post("/api/v1/auth/password/forgot")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").exists())
        }

        // Validates: Requirement 4.2
        @Test
        fun `should validate valid reset token and return 200`() {
            val registration = testData.registerChurch(adminEmail = "reset-valid@testchurch.com")

            // Create a valid password reset token
            val token = PasswordResetToken(
                userId = registration.adminUserId,
                token = "valid-reset-token-${UUID.randomUUID()}",
                email = "reset-valid@testchurch.com",
                expiresAt = LocalDateTime.now().plusHours(1)
            )
            passwordResetTokenRepository.save(token)

            mockMvc.perform(
                get("/api/v1/auth/password/reset/${token.token}/validate")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Token is valid"))
        }

        // Validates: Requirement 4.3
        @Test
        fun `should return 400 for expired reset token`() {
            val registration = testData.registerChurch(adminEmail = "reset-expired@testchurch.com")

            // Create an expired token
            val token = PasswordResetToken(
                userId = registration.adminUserId,
                token = "expired-reset-token-${UUID.randomUUID()}",
                email = "reset-expired@testchurch.com",
                expiresAt = LocalDateTime.now().minusHours(1)
            )
            passwordResetTokenRepository.save(token)

            mockMvc.perform(
                get("/api/v1/auth/password/reset/${token.token}/validate")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Reset token has expired"))
        }

        // Validates: Requirement 4.4
        @Test
        fun `should reset password with valid token and return 200`() {
            val registration = testData.registerChurch(adminEmail = "reset-pw@testchurch.com")

            val token = PasswordResetToken(
                userId = registration.adminUserId,
                token = "reset-pw-token-${UUID.randomUUID()}",
                email = "reset-pw@testchurch.com",
                expiresAt = LocalDateTime.now().plusHours(1)
            )
            passwordResetTokenRepository.save(token)

            val request = mapOf(
                "token" to token.token,
                "newPassword" to "NewSecurePass123!"
            )

            mockMvc.perform(
                post("/api/v1/auth/password/reset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
        }

        // Validates: Requirement 4.5
        @Test
        fun `should return 400 when resetting password with weak password`() {
            val registration = testData.registerChurch(adminEmail = "reset-weak@testchurch.com")

            val token = PasswordResetToken(
                userId = registration.adminUserId,
                token = "reset-weak-token-${UUID.randomUUID()}",
                email = "reset-weak@testchurch.com",
                expiresAt = LocalDateTime.now().plusHours(1)
            )
            passwordResetTokenRepository.save(token)

            val request = mapOf(
                "token" to token.token,
                "newPassword" to TestConstants.WEAK_PASSWORD
            )

            mockMvc.perform(
                post("/api/v1/auth/password/reset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        // Validates: Requirement 4.6
        @Test
        fun `should set password for OAuth user and return 200`() {
            val registration = testData.registerChurch(adminEmail = "setup-oauth@testchurch.com")

            // Create an OAuth user (no password hash)
            val oauthUser = User(
                email = "oauth-user@testchurch.com",
                firstName = "OAuth",
                lastName = "User",
                passwordHash = null,
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedUser = userRepository.save(oauthUser)

            val request = mapOf("password" to TestConstants.VALID_PASSWORD)

            mockMvc.perform(
                post("/api/v1/auth/password/set")
                    .with(
                        TestSecurityHelper.withAuth(
                            savedUser.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").exists())
        }

        // Validates: Requirement 4.7
        @Test
        fun `should change password with correct current password and return 200`() {
            val email = "change-pw@testchurch.com"
            val currentPassword = TestConstants.VALID_PASSWORD
            val registration = testData.registerChurch(adminEmail = email, adminPassword = currentPassword)

            val request = mapOf(
                "currentPassword" to currentPassword,
                "newPassword" to "NewSecurePass456!"
            )

            mockMvc.perform(
                put("/api/v1/auth/password/change")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
        }

        // Validates: Requirement 4.8
        @Test
        fun `should return 400 when changing password with incorrect current password`() {
            val email = "change-pw-wrong@testchurch.com"
            val registration = testData.registerChurch(adminEmail = email)

            val request = mapOf(
                "currentPassword" to "WrongCurrentPassword!",
                "newPassword" to "NewSecurePass456!"
            )

            mockMvc.perform(
                put("/api/v1/auth/password/change")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
        }

        // Validates: Requirement 4.9
        @Test
        fun `should check password status and return 200 with status fields`() {
            val registration = testData.registerChurch(adminEmail = "pw-status@testchurch.com")

            mockMvc.perform(
                get("/api/v1/auth/password/status")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.hasPassword").isBoolean)
                .andExpect(jsonPath("$.canSetPassword").isBoolean)
                .andExpect(jsonPath("$.canChangePassword").isBoolean)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 5: Invitations
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 5 - Invitations")
    inner class Invitations {

        // Validates: Requirement 5.1
        @Test
        fun `should send invitation with valid data and return 201 with invitationId`() {
            val registration = testData.registerChurch(adminEmail = "invite-admin@testchurch.com")

            val request = mapOf(
                "email" to "invitee@testchurch.com",
                "firstName" to "Invited",
                "lastName" to "User",
                "role" to "TEAM_MEMBER"
            )

            mockMvc.perform(
                post("/api/v1/invitations/send")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.invitationId").exists())
        }

        // Validates: Requirement 5.2
        @Test
        fun `should return 409 when sending invitation to existing email`() {
            val registration = testData.registerChurch(adminEmail = "invite-dup-admin@testchurch.com")

            // Create a user with the target email
            val existingUser = User(
                email = "existing-invitee@testchurch.com",
                firstName = "Existing",
                lastName = "User",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            userRepository.save(existingUser)

            val request = mapOf(
                "email" to "existing-invitee@testchurch.com",
                "firstName" to "Existing",
                "lastName" to "User",
                "role" to "TEAM_MEMBER"
            )

            mockMvc.perform(
                post("/api/v1/invitations/send")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
        }

        // Validates: Requirement 5.3
        @Test
        fun `should get invitation details and return 200 with all fields`() {
            val registration = testData.registerChurch(adminEmail = "invite-details-admin@testchurch.com")

            // Create an invitation token directly
            val invitation = InvitationToken(
                token = "invite-details-token-${UUID.randomUUID()}",
                email = "details-invitee@testchurch.com",
                firstName = "Detail",
                lastName = "Invitee",
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                invitedBy = registration.adminUserId,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
            invitationTokenRepository.save(invitation)

            mockMvc.perform(
                get("/api/v1/invitations/${invitation.token}")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.email").value("details-invitee@testchurch.com"))
                .andExpect(jsonPath("$.firstName").value("Detail"))
                .andExpect(jsonPath("$.lastName").value("Invitee"))
                .andExpect(jsonPath("$.churchName").exists())
                .andExpect(jsonPath("$.role").value("TEAM_MEMBER"))
                .andExpect(jsonPath("$.expiresAt").exists())
        }

        // Validates: Requirement 5.4
        @Test
        fun `should accept invitation with valid token and password and return 201 with userId`() {
            val registration = testData.registerChurch(adminEmail = "invite-accept-admin@testchurch.com")

            val invitation = InvitationToken(
                token = "invite-accept-token-${UUID.randomUUID()}",
                email = "accept-invitee@testchurch.com",
                firstName = "Accept",
                lastName = "Invitee",
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                invitedBy = registration.adminUserId,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
            invitationTokenRepository.save(invitation)

            val request = mapOf("password" to TestConstants.VALID_PASSWORD)

            mockMvc.perform(
                post("/api/v1/invitations/${invitation.token}/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.message").exists())
        }

        // Validates: Requirement 5.5
        @Test
        fun `should return 400 when accepting invitation with expired token`() {
            val registration = testData.registerChurch(adminEmail = "invite-expired-admin@testchurch.com")

            val invitation = InvitationToken(
                token = "invite-expired-token-${UUID.randomUUID()}",
                email = "expired-invitee@testchurch.com",
                firstName = "Expired",
                lastName = "Invitee",
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                invitedBy = registration.adminUserId,
                expiresAt = LocalDateTime.now().minusDays(1) // Already expired
            )
            invitationTokenRepository.save(invitation)

            val request = mapOf("password" to TestConstants.VALID_PASSWORD)

            mockMvc.perform(
                post("/api/v1/invitations/${invitation.token}/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Invitation has expired"))
        }

        // Validates: Requirement 5.6
        @Test
        fun `should return 400 when accepting invitation with weak password`() {
            val registration = testData.registerChurch(adminEmail = "invite-weakpw-admin@testchurch.com")

            val invitation = InvitationToken(
                token = "invite-weakpw-token-${UUID.randomUUID()}",
                email = "weakpw-invitee@testchurch.com",
                firstName = "WeakPw",
                lastName = "Invitee",
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                invitedBy = registration.adminUserId,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
            invitationTokenRepository.save(invitation)

            val request = mapOf("password" to TestConstants.WEAK_PASSWORD)

            mockMvc.perform(
                post("/api/v1/invitations/${invitation.token}/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        // Validates: Requirement 5.7
        @Test
        fun `should return 400 when accepting invitation with already-used token`() {
            val registration = testData.registerChurch(adminEmail = "invite-used-admin@testchurch.com")

            val invitation = InvitationToken(
                token = "invite-used-token-${UUID.randomUUID()}",
                email = "used-invitee@testchurch.com",
                firstName = "Used",
                lastName = "Invitee",
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                invitedBy = registration.adminUserId,
                expiresAt = LocalDateTime.now().plusDays(7),
                isUsed = true // Already used
            )
            invitationTokenRepository.save(invitation)

            val request = mapOf("password" to TestConstants.VALID_PASSWORD)

            mockMvc.perform(
                post("/api/v1/invitations/${invitation.token}/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Invitation has already been used"))
        }
    }
}
