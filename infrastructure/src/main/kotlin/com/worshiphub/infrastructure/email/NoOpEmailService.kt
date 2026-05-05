package com.worshiphub.infrastructure.email

import com.worshiphub.application.auth.EmailService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * No-op [EmailService] for the H2 (development/testing) profile.
 *
 * When running with `--spring.profiles.active=h2`, this bean takes priority
 * over [EmailServiceImpl] so that church registration and other flows that
 * send emails do not fail when no real mail server is available.
 *
 * All methods log the action at INFO level for debugging but perform no
 * actual email delivery.
 */
@Service
@Profile("h2")
class NoOpEmailService : EmailService {

    private val logger = LoggerFactory.getLogger(NoOpEmailService::class.java)

    override fun sendEmailVerification(email: String, firstName: String, token: String) {
        logger.info("[NoOp] Would send email verification to {} (token={})", email, token)
    }

    override fun sendPasswordReset(email: String, firstName: String, token: String) {
        logger.info("[NoOp] Would send password reset to {} (token={})", email, token)
    }

    override fun sendInvitation(email: String, firstName: String, churchName: String, invitationToken: String) {
        logger.info("[NoOp] Would send invitation to {} for church {}", email, churchName)
    }

    override fun sendWelcomeEmail(email: String, firstName: String, churchName: String) {
        logger.info("[NoOp] Would send welcome email to {} for church {}", email, churchName)
    }
}
