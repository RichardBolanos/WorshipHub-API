package com.worshiphub.domain.auth.repository

import com.worshiphub.domain.auth.InvitationToken
import java.util.*

/**
 * Repository interface for invitation token operations.
 */
interface InvitationTokenRepository {
    
    /**
     * Saves an invitation token.
     */
    fun save(invitation: InvitationToken): InvitationToken
    
    /**
     * Finds an invitation by its ID.
     */
    fun findById(id: UUID): InvitationToken?
    
    /**
     * Finds an invitation by its token string.
     */
    fun findByToken(token: String): InvitationToken?
    
    /**
     * Finds all valid invitations for an email.
     */
    fun findValidInvitationsByEmail(email: String): List<InvitationToken>
    
    /**
     * Finds all invitations sent by a user.
     */
    fun findInvitationsByInvitedBy(invitedBy: UUID): List<InvitationToken>
    
    /**
     * Invalidates all invitations for an email (marks as used).
     */
    fun invalidateAllInvitationsForEmail(email: String)
    
    /**
     * Deletes expired invitations (cleanup job).
     */
    fun deleteExpiredInvitations()
}