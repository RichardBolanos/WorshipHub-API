package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.auth.PasswordResetToken
import com.worshiphub.domain.auth.repository.PasswordResetTokenRepository
import com.worshiphub.infrastructure.persistence.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Spring Data JPA repository for password reset tokens.
 */
interface JpaPasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, UUID> {
    
    fun findByToken(token: String): PasswordResetTokenEntity?
    
    @Query("SELECT p FROM PasswordResetTokenEntity p WHERE p.userId = :userId AND p.isUsed = false AND p.expiresAt > :now")
    fun findValidTokensByUserId(userId: UUID, now: LocalDateTime = LocalDateTime.now()): List<PasswordResetTokenEntity>
    
    @Modifying
    @Query("UPDATE PasswordResetTokenEntity p SET p.isUsed = true WHERE p.userId = :userId AND p.isUsed = false")
    fun invalidateAllTokensForUser(userId: UUID)
    
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity p WHERE p.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime = LocalDateTime.now())
}

/**
 * Implementation of domain repository using Spring Data JPA.
 */
@Repository
open class PasswordResetTokenRepositoryImpl(
    private val jpaRepository: JpaPasswordResetTokenRepository
) : PasswordResetTokenRepository {
    
    override fun save(token: PasswordResetToken): PasswordResetToken {
        val entity = PasswordResetTokenEntity.fromDomain(token)
        return jpaRepository.save(entity).toDomain()
    }
    
    override fun findByToken(token: String): PasswordResetToken? {
        return jpaRepository.findByToken(token)?.toDomain()
    }
    
    override fun findValidTokensByUserId(userId: UUID): List<PasswordResetToken> {
        return jpaRepository.findValidTokensByUserId(userId).map { it.toDomain() }
    }
    
    override fun invalidateAllTokensForUser(userId: UUID) {
        jpaRepository.invalidateAllTokensForUser(userId)
    }
    
    override fun deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens()
    }
}