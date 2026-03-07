package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.auth.PasswordResetToken
import com.worshiphub.domain.auth.repository.PasswordResetTokenRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

interface JpaPasswordResetTokenRepository : JpaRepository<PasswordResetToken, UUID> {
    fun findByToken(token: String): PasswordResetToken?
    
    @Query("SELECT p FROM PasswordResetToken p WHERE p.userId = :userId AND p.isUsed = false AND p.expiresAt > :now")
    fun findValidTokensByUserId(userId: UUID, now: LocalDateTime = LocalDateTime.now()): List<PasswordResetToken>
    
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.userId = :userId AND p.isUsed = false")
    fun invalidateAllTokensForUser(userId: UUID)
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime = LocalDateTime.now())
}

@Repository
open class PasswordResetTokenRepositoryImpl(
    private val jpaRepository: JpaPasswordResetTokenRepository
) : PasswordResetTokenRepository {
    
    override fun save(token: PasswordResetToken): PasswordResetToken = jpaRepository.save(token)
    
    override fun findByToken(token: String): PasswordResetToken? = jpaRepository.findByToken(token)
    
    override fun findValidTokensByUserId(userId: UUID): List<PasswordResetToken> = 
        jpaRepository.findValidTokensByUserId(userId)
    
    override fun invalidateAllTokensForUser(userId: UUID) {
        jpaRepository.invalidateAllTokensForUser(userId)
    }
    
    override fun deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens()
    }
}