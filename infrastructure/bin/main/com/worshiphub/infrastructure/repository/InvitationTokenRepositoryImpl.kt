package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.auth.InvitationToken
import com.worshiphub.domain.auth.repository.InvitationTokenRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

interface JpaInvitationTokenRepository : JpaRepository<InvitationToken, UUID> {
    fun findByToken(token: String): InvitationToken?
    
    @Query("SELECT i FROM InvitationToken i WHERE i.email = :email AND i.isUsed = false AND i.expiresAt > :now")
    fun findValidInvitationsByEmail(email: String, now: LocalDateTime = LocalDateTime.now()): List<InvitationToken>
    
    fun findByInvitedByOrderByCreatedAtDesc(invitedBy: UUID): List<InvitationToken>
    
    @Modifying
    @Query("UPDATE InvitationToken i SET i.isUsed = true WHERE i.email = :email AND i.isUsed = false")
    fun invalidateAllInvitationsForEmail(email: String)
    
    @Modifying
    @Query("DELETE FROM InvitationToken i WHERE i.expiresAt < :now")
    fun deleteExpiredInvitations(now: LocalDateTime = LocalDateTime.now())
}

@Repository
open class InvitationTokenRepositoryImpl(
    private val jpaRepository: JpaInvitationTokenRepository
) : InvitationTokenRepository {
    
    override fun save(invitation: InvitationToken): InvitationToken = jpaRepository.save(invitation)
    
    override fun findById(id: UUID): InvitationToken? = jpaRepository.findById(id).orElse(null)
    
    override fun findByToken(token: String): InvitationToken? = jpaRepository.findByToken(token)
    
    override fun findValidInvitationsByEmail(email: String): List<InvitationToken> = 
        jpaRepository.findValidInvitationsByEmail(email)
    
    override fun findInvitationsByInvitedBy(invitedBy: UUID): List<InvitationToken> = 
        jpaRepository.findByInvitedByOrderByCreatedAtDesc(invitedBy)
    
    override fun invalidateAllInvitationsForEmail(email: String) {
        jpaRepository.invalidateAllInvitationsForEmail(email)
    }
    
    override fun deleteExpiredInvitations() {
        jpaRepository.deleteExpiredInvitations()
    }
}