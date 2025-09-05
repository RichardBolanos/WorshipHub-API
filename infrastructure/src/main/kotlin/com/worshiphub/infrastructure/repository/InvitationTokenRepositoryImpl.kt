package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.auth.InvitationToken
import com.worshiphub.domain.auth.repository.InvitationTokenRepository
import com.worshiphub.infrastructure.persistence.InvitationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Spring Data JPA repository for invitation tokens.
 */
interface JpaInvitationTokenRepository : JpaRepository<InvitationTokenEntity, UUID> {
    
    fun findByToken(token: String): InvitationTokenEntity?
    
    @Query("SELECT i FROM InvitationTokenEntity i WHERE i.email = :email AND i.isUsed = false AND i.expiresAt > :now")
    fun findValidInvitationsByEmail(email: String, now: LocalDateTime = LocalDateTime.now()): List<InvitationTokenEntity>
    
    fun findByInvitedByOrderByCreatedAtDesc(invitedBy: UUID): List<InvitationTokenEntity>
    
    @Modifying
    @Query("UPDATE InvitationTokenEntity i SET i.isUsed = true WHERE i.email = :email AND i.isUsed = false")
    fun invalidateAllInvitationsForEmail(email: String)
    
    @Modifying
    @Query("DELETE FROM InvitationTokenEntity i WHERE i.expiresAt < :now")
    fun deleteExpiredInvitations(now: LocalDateTime = LocalDateTime.now())
}

/**
 * Implementation of domain repository using Spring Data JPA.
 */
@Repository
open class InvitationTokenRepositoryImpl(
    private val jpaRepository: JpaInvitationTokenRepository
) : InvitationTokenRepository {
    
    override fun save(invitation: InvitationToken): InvitationToken {
        val entity = InvitationTokenEntity.fromDomain(invitation)
        return jpaRepository.save(entity).toDomain()
    }
    
    override fun findByToken(token: String): InvitationToken? {
        return jpaRepository.findByToken(token)?.toDomain()
    }
    
    override fun findValidInvitationsByEmail(email: String): List<InvitationToken> {
        return jpaRepository.findValidInvitationsByEmail(email).map { it.toDomain() }
    }
    
    override fun findInvitationsByInvitedBy(invitedBy: UUID): List<InvitationToken> {
        return jpaRepository.findByInvitedByOrderByCreatedAtDesc(invitedBy).map { it.toDomain() }
    }
    
    override fun invalidateAllInvitationsForEmail(email: String) {
        jpaRepository.invalidateAllInvitationsForEmail(email)
    }
    
    override fun deleteExpiredInvitations() {
        jpaRepository.deleteExpiredInvitations()
    }
}