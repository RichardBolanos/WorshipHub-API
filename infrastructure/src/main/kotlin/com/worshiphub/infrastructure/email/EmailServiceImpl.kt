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
    @Value("\${app.base-url:}") private val baseUrl: String,
    // Sender (From: header). MUST be a verified sender in your SMTP provider
    // (Brevo: https://app.brevo.com/senders/list). It is INTENTIONALLY decoupled
    // from spring.mail.username — many providers (Brevo, SendGrid, AWS SES) use
    // a non-routable login like xxxxx@smtp-brevo.com that can never appear as
    // a real sender address.
    //
    // No fallback: if APP_EMAIL_FROM is not set, the application logs a clear
    // error and skips the send. This is preferable to using a placeholder like
    // "noreply@worshiphub.com" which would be silently rejected by the SMTP
    // provider as an unverified sender, leaving operators wondering why
    // emails do not arrive.
    @Value("\${app.email.from:}") private val fromEmail: String
) : EmailService {

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
        if (fromEmail.isBlank()) {
            logger.error(
                "Cannot send email to {} (subject='{}') — APP_EMAIL_FROM is not configured. " +
                    "Set it to a verified sender address in your SMTP provider.",
                sanitizeForLog(to),
                subject
            )
            return
        }
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
            // trigger a "resend" flow. We log the failure for ops visibility,
            // with a hint when the cause matches a well-known deployment pitfall.
            val hint = diagnoseSmtpFailure(e)
            logger.error(
                "Failed to send email to {} (subject='{}'). Email NOT sent — " +
                    "the user-facing operation continues without rollback.{}",
                sanitizeForLog(to),
                subject,
                if (hint.isNotEmpty()) " HINT: $hint" else "",
                e
            )
        }
    }

    /**
     * Best-effort, human-readable diagnosis for the most common SMTP failures
     * we have seen in production. Returned string is appended to the ERROR
     * log so on-call engineers can fix the issue without scrolling stack
     * traces or grepping documentation.
     */
    private fun diagnoseSmtpFailure(e: Throwable): String {
        // Walk the cause chain — the root cause is what actually identifies
        // the failure mode (Spring wraps it in MailSendException).
        var current: Throwable? = e
        while (current != null) {
            val msg = current.message ?: ""
            when {
                current is java.net.SocketTimeoutException &&
                    msg.contains("Connect timed out", ignoreCase = true) ->
                    return "TCP connect to the SMTP host timed out. " +
                        "Most cloud hosts (Render, Heroku, Fly, Railway) block outbound port 587. " +
                        "Try SPRING_MAIL_PORT=2525 (Brevo's alternate port; SendGrid also supports 2525)."
                msg.contains("Authentication failed", ignoreCase = true) ||
                    msg.contains("535", ignoreCase = false) ->
                    return "SMTP authentication rejected by the server. " +
                        "Double-check SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD. " +
                        "For Brevo: USERNAME is the 'Iniciar sesion' value (xxxxxxx@smtp-brevo.com), " +
                        "PASSWORD is a Clave SMTP (NOT your account password)."
                msg.contains("sender", ignoreCase = true) &&
                    (msg.contains("not allowed", ignoreCase = true) ||
                        msg.contains("not verified", ignoreCase = true)) ->
                    return "The 'From:' address is not a verified sender. " +
                        "Check APP_EMAIL_FROM matches a sender verified at " +
                        "https://app.brevo.com/senders/list (or your provider's equivalent)."
                msg.contains("UnknownHostException", ignoreCase = true) ->
                    return "DNS resolution failed for the SMTP host. " +
                        "Verify SPRING_MAIL_HOST is spelled correctly."
            }
            current = current.cause
        }
        return ""
    }

    private fun sanitizeForLog(input: String): String {
        return input.replace("[\r\n\t]".toRegex(), "_")
    }
}