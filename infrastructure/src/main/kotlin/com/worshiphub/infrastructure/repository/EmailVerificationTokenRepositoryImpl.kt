package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.auth.EmailVerificationToken
import com.worshiphub.domain.auth.repository.EmailVerificationTokenRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

interface JpaEmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, UUID> {
    fun findByToken(token: String): EmailVerificationToken?
    
    @Query("SELECT e FROM EmailVerificationToken e WHERE e.userId = :userId AND e.isUsed = false AND e.expiresAt > :now")
    fun findValidTokensByUserId(userId: UUID, now: LocalDateTime = LocalDateTime.now()): List<EmailVerificationToken>
    
    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.isUsed = true WHERE e.userId = :userId AND e.isUsed = false")
    fun invalidateAllTokensForUser(userId: UUID)
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime = LocalDateTime.now())
}

@Repository
open class EmailVerificationTokenRepositoryImpl(
    private val jpaRepository: JpaEmailVerificationTokenRepository
) : EmailVerificationTokenRepository {
    
    override fun save(token: EmailVerificationToken): EmailVerificationToken = jpaRepository.save(token)
    
    override fun findByToken(token: String): EmailVerificationToken? = jpaRepository.findByToken(token)
    
    override fun findValidTokensByUserId(userId: UUID): List<EmailVerificationToken> = 
        jpaRepository.findValidTokensByUserId(userId)
    
    override fun invalidateAllTokensForUser(userId: UUID) {
        jpaRepository.invalidateAllTokensForUser(userId)
    }
    
    override fun deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens()
    }
}