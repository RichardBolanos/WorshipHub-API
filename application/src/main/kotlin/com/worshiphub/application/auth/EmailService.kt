package com.worshiphub.application.auth

/**
 * Interface for email operations.
 * Implementation will be provided in infrastructure layer.
 */
interface EmailService {
    
    /**
     * Sends email verification message.
     */
    fun sendEmailVerification(email: String, firstName: String, token: String)
    
    /**
     * Sends password reset email.
     */
    fun sendPasswordReset(email: String, firstName: String, token: String)
    
    /**
     * Sends invitation email.
     */
    fun sendInvitation(email: String, firstName: String, churchName: String, invitationToken: String)
    
    /**
     * Sends welcome email after successful registration.
     */
    fun sendWelcomeEmail(email: String, firstName: String, churchName: String)
}