package com.worshiphub.infrastructure.email

import com.worshiphub.application.auth.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Email service implementation.
 * For development: logs emails to console.
 * For production: implement with actual SMTP service.
 */
@Service
class EmailServiceImpl(
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String
) : EmailService {
    
    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)
    
    override fun sendEmailVerification(email: String, firstName: String, token: String) {
        val verificationUrl = "$baseUrl/api/v1/auth/email/verify/$token"
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== EMAIL VERIFICATION ===
                |To: $email
                |Subject: Verify your WorshipHub account
                |
                |Hi $firstName,
                |
                |Please verify your email address by clicking the link below:
                |$verificationUrl
                |
                |This link will expire in 24 hours.
                |
                |Best regards,
                |WorshipHub Team
                |===========================
            """.trimMargin())
        } else {
            // TODO: Implement actual email sending for production
            logger.info("Email verification sent to $email")
        }
    }
    
    override fun sendPasswordReset(email: String, firstName: String, token: String) {
        val resetUrl = "$baseUrl/reset-password?token=$token"
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== PASSWORD RESET ===
                |To: $email
                |Subject: Reset your WorshipHub password
                |
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
                |======================
            """.trimMargin())
        } else {
            // TODO: Implement actual email sending for production
            logger.info("Password reset email sent to $email")
        }
    }
    
    override fun sendInvitation(email: String, firstName: String, churchName: String, invitationToken: String) {
        val invitationUrl = "$baseUrl/invitations/$invitationToken"
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== INVITATION ===
                |To: $email
                |Subject: You're invited to join $churchName on WorshipHub
                |
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
                |==================
            """.trimMargin())
        } else {
            // TODO: Implement actual email sending for production
            logger.info("Invitation email sent to $email for church $churchName")
        }
    }
    
    override fun sendWelcomeEmail(email: String, firstName: String, churchName: String) {
        if (activeProfile == "dev") {
            logger.info("""
                |=== WELCOME EMAIL ===
                |To: $email
                |Subject: Welcome to WorshipHub!
                |
                |Hi $firstName,
                |
                |Welcome to WorshipHub! Your church $churchName has been successfully registered.
                |
                |Please verify your email address to complete the setup.
                |
                |Best regards,
                |WorshipHub Team
                |=====================
            """.trimMargin())
        } else {
            // TODO: Implement actual email sending for production
            logger.info("Welcome email sent to $email for church $churchName")
        }
    }
}