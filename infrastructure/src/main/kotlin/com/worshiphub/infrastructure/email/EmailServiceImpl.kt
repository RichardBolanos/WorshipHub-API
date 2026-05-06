package com.worshiphub.infrastructure.email

import com.worshiphub.application.auth.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Email service implementation using JavaMailSender.
 *
 * Design notes:
 * - All public methods are [@Async] so they do NOT block the HTTP request thread.
 *   A user registering, requesting a password reset, or being invited will get
 *   their HTTP response back immediately; the email is sent in a background
 *   thread pool (see AsyncPushConfig).
 * - Errors during SMTP send are logged but NEVER propagated. A failure to
 *   deliver an email must NOT roll back the database transaction nor surface
 *   as an HTTP 500 to the user. The user can always trigger a "resend" from
 *   the auth UI.
 */
@Service
@Profile("!h2")
class EmailServiceImpl(
    private val mailSender: JavaMailSender,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    // Sender (From: header). MUST be a verified sender in your SMTP provider
    // (Brevo: https://app.brevo.com/senders/list). It is INTENTIONALLY decoupled
    // from spring.mail.username — many providers (Brevo, SendGrid, AWS SES) use
    // a non-routable login like xxxxx@smtp-brevo.com that can never appear as
    // a real sender address.
    //
    // Resolution order:
    //   1. APP_EMAIL_FROM (preferred, explicit)
    //   2. spring.mail.username (legacy fallback for providers where login == sender)
    //   3. noreply@worshiphub.com (last resort default; emails likely rejected)
    @Value("\${app.email.from:}") private val configuredFrom: String,
    @Value("\${spring.mail.username:noreply@worshiphub.com}") private val fallbackFrom: String
) : EmailService {

    private val fromEmail: String
        get() = configuredFrom.ifBlank { fallbackFrom }
    
    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)
    
    @Async
    override fun sendEmailVerification(email: String, firstName: String, token: String) {
        val verificationUrl = "$baseUrl/api/v1/auth/email/verify/$token"
        
        val text = """
            |Hi $firstName,
            |
            |Please verify your email address by clicking the link below:
            |$verificationUrl
            |
            |This link will expire in 24 hours.
            |
            |Best regards,
            |WorshipHub Team
        """.trimMargin()
        
        sendEmail(
            to = email,
            subject = "Verify your WorshipHub account",
            text = text
        )
    }
    
    @Async
    override fun sendPasswordReset(email: String, firstName: String, token: String) {
        val resetUrl = "$baseUrl/reset-password?token=$token"
        
        val text = """
            |Hi $firstName,
            |
            |You requested to reset your password. Click the link below:
            |$resetUrl
            |
            |This link will expire in 1 hour.
            |If you didn't request this, please ignore this email.
            |
            |Best regards,
            |WorshipHub Team
        """.trimMargin()
        
        sendEmail(
            to = email,
            subject = "Reset your WorshipHub password",
            text = text
        )
    }
    
    @Async
    override fun sendInvitation(email: String, firstName: String, churchName: String, invitationToken: String) {
        val invitationUrl = "$baseUrl/invitations/$invitationToken"

        val text = """
            |Hi $firstName,
            |
            |You've been invited to join $churchName on WorshipHub!
            |Click the link below to view and accept the invitation:
            |$invitationUrl
            |
            |This invitation will expire in 7 days.
            |
            |Best regards,
            |WorshipHub Team
        """.trimMargin()
        
        sendEmail(
            to = email,
            subject = "You're invited to join $churchName on WorshipHub",
            text = text
        )
    }
    
    @Async
    override fun sendWelcomeEmail(email: String, firstName: String, churchName: String) {
        val text = """
            |Hi $firstName,
            |
            |Welcome to WorshipHub! Your church $churchName has been successfully registered.
            |
            |Please verify your email address to complete the setup.
            |
            |Best regards,
            |WorshipHub Team
        """.trimMargin()
        
        sendEmail(
            to = email,
            subject = "Welcome to WorshipHub!",
            text = text
        )
    }

    private fun sendEmail(to: String, subject: String, text: String) {
        try {
            val message = SimpleMailMessage()
            message.from = fromEmail
            message.setTo(to)
            message.subject = subject
            message.text = text
            
            mailSender.send(message)
            logger.info("Email sent successfully to: {}", sanitizeForLog(to))
        } catch (e: Exception) {
            // SMTP failures must NOT break the surrounding business operation
            // (e.g. user registration, invitation creation). The user can always
            // trigger a "resend" flow. We log the failure for ops visibility.
            logger.error(
                "Failed to send email to {} (subject='{}'). Email NOT sent — " +
                    "the user-facing operation continues without rollback.",
                sanitizeForLog(to),
                subject,
                e
            )
        }
    }
    
    private fun sanitizeForLog(input: String): String {
        return input.replace("[\r\n\t]".toRegex(), "_")
    }
}