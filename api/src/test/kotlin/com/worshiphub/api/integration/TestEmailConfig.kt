package com.worshiphub.api.integration

import com.worshiphub.application.auth.EmailService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration that provides a no-op [EmailService] implementation.
 * This prevents actual email sending during E2E tests while allowing
 * the full Spring context to load without a real mail server.
 */
@TestConfiguration
class TestEmailConfig {

    @Bean
    @Primary
    fun testEmailService(): EmailService = object : EmailService {
        override fun sendEmailVerification(email: String, firstName: String, token: String) {
            // No-op for tests
        }

        override fun sendPasswordReset(email: String, firstName: String, token: String) {
            // No-op for tests
        }

        override fun sendInvitation(email: String, firstName: String, churchName: String, invitationToken: String) {
            // No-op for tests
        }

        override fun sendWelcomeEmail(email: String, firstName: String, churchName: String) {
            // No-op for tests
        }
    }
}
