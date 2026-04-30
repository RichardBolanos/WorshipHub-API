package com.worshiphub.infrastructure.email

import com.worshiphub.application.auth.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

/**
 * Email service implementation using JavaMailSender.
 * Real emails will be sent (or routed to Mailpit in local environment).
 */
@Service
@Profile("!h2")
class EmailServiceImpl(
    private val mailSender: JavaMailSender,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${spring.mail.username:noreply@worshiphub.com}") private val fromEmail: String
) : EmailService {
    
    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)
    
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
            logger.error("Failed to send email to {}", sanitizeForLog(to), e)
            // Depending on requirements, we might throw an exception here
            // or just log it and continue. For auth flows, we usually want to know if it failed.
            throw RuntimeException("Failed to send email", e)
        }
    }
    
    private fun sanitizeForLog(input: String): String {
        return input.replace("[\r\n\t]".toRegex(), "_")
    }
}