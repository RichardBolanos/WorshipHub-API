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
        val sanitizedEmail = sanitizeForLog(email)
        val sanitizedFirstName = sanitizeForLog(firstName)
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== EMAIL VERIFICATION ===
                |To: {}
                |Subject: Verify your WorshipHub account
                |
                |Hi {},
                |
                |Please verify your email address by clicking the link below:
                |{}
                |
                |This link will expire in 24 hours.
                |
                |Best regards,
                |WorshipHub Team
                |===========================
            """.trimMargin(), sanitizedEmail, sanitizedFirstName, verificationUrl)
        } else {
            logger.info("Email verification sent to {}", sanitizedEmail)
        }
    }
    
    override fun sendPasswordReset(email: String, firstName: String, token: String) {
        val resetUrl = "$baseUrl/reset-password?token=$token"
        val sanitizedEmail = sanitizeForLog(email)
        val sanitizedFirstName = sanitizeForLog(firstName)
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== PASSWORD RESET ===
                |To: {}
                |Subject: Reset your WorshipHub password
                |
                |Hi {},
                |
                |You requested to reset your password. Click the link below:
                |{}
                |
                |This link will expire in 1 hour.
                |If you didn't request this, please ignore this email.
                |
                |Best regards,
                |WorshipHub Team
                |======================
            """.trimMargin(), sanitizedEmail, sanitizedFirstName, resetUrl)
        } else {
            logger.info("Password reset email sent to {}", sanitizedEmail)
        }
    }
    
    override fun sendInvitation(email: String, firstName: String, churchName: String, invitationToken: String) {
        val invitationUrl = "$baseUrl/invitations/$invitationToken"
        val sanitizedEmail = sanitizeForLog(email)
        val sanitizedFirstName = sanitizeForLog(firstName)
        val sanitizedChurchName = sanitizeForLog(churchName)
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== INVITATION ===
                |To: {}
                |Subject: You're invited to join {} on WorshipHub
                |
                |Hi {},
                |
                |You've been invited to join {} on WorshipHub!
                |Click the link below to view and accept the invitation:
                |{}
                |
                |This invitation will expire in 7 days.
                |
                |Best regards,
                |WorshipHub Team
                |==================
            """.trimMargin(), sanitizedEmail, sanitizedChurchName, sanitizedFirstName, sanitizedChurchName, invitationUrl)
        } else {
            logger.info("Invitation email sent to {} for church {}", sanitizedEmail, sanitizedChurchName)
        }
    }
    
    override fun sendWelcomeEmail(email: String, firstName: String, churchName: String) {
        val sanitizedEmail = sanitizeForLog(email)
        val sanitizedFirstName = sanitizeForLog(firstName)
        val sanitizedChurchName = sanitizeForLog(churchName)
        
        if (activeProfile == "dev") {
            logger.info("""
                |=== WELCOME EMAIL ===
                |To: {}
                |Subject: Welcome to WorshipHub!
                |
                |Hi {},
                |
                |Welcome to WorshipHub! Your church {} has been successfully registered.
                |
                |Please verify your email address to complete the setup.
                |
                |Best regards,
                |WorshipHub Team
                |=====================
            """.trimMargin(), sanitizedEmail, sanitizedFirstName, sanitizedChurchName)
        } else {
            logger.info("Welcome email sent to {} for church {}", sanitizedEmail, sanitizedChurchName)
        }
    }
    
    private fun sanitizeForLog(input: String): String {
        return input.replace("[\r\n\t]".toRegex(), "_")
    }
}