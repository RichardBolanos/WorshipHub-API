package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.auth.EmailVerificationToken
import com.worshiphub.domain.auth.repository.EmailVerificationTokenRepository
import com.worshiphub.infrastructure.persistence.EmailVerificationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Spring Data JPA repository for email verification tokens.
 */
interface JpaEmailVerificationTokenRepository : JpaRepository<EmailVerificationTokenEntity, UUID> {
    
    fun findByToken(token: String): EmailVerificationTokenEntity?
    
    @Query("SELECT e FROM EmailVerificationTokenEntity e WHERE e.userId = :userId AND e.isUsed = false AND e.expiresAt > :now")
    fun findValidTokensByUserId(userId: UUID, now: LocalDateTime = LocalDateTime.now()): List<EmailVerificationTokenEntity>
    
    @Modifying
    @Query("UPDATE EmailVerificationTokenEntity e SET e.isUsed = true WHERE e.userId = :userId AND e.isUsed = false")
    fun invalidateAllTokensForUser(userId: UUID)
    
    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity e WHERE e.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime = LocalDateTime.now())
}

/**
 * Implementation of domain repository using Spring Data JPA.
 */
@Repository
open class EmailVerificationTokenRepositoryImpl(
    private val jpaRepository: JpaEmailVerificationTokenRepository
) : EmailVerificationTokenRepository {
    
    override fun save(token: EmailVerificationToken): EmailVerificationToken {
        val entity = EmailVerificationTokenEntity.fromDomain(token)
        return jpaRepository.save(entity).toDomain()
    }
    
    override fun findByToken(token: String): EmailVerificationToken? {
        return jpaRepository.findByToken(token)?.toDomain()
    }
    
    override fun findValidTokensByUserId(userId: UUID): List<EmailVerificationToken> {
        return jpaRepository.findValidTokensByUserId(userId).map { it.toDomain() }
    }
    
    override fun invalidateAllTokensForUser(userId: UUID) {
        jpaRepository.invalidateAllTokensForUser(userId)
    }
    
    override fun deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens()
    }
}