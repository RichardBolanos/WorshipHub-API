package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.push.DeviceToken
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Spring Data JPA interface for DeviceToken persistence operations.
 */
@Repository
interface SpringDataDeviceTokenRepository : JpaRepository<DeviceToken, UUID> {
    fun findByUserId(userId: UUID): List<DeviceToken>
    fun findByToken(token: String): DeviceToken?
    fun deleteByToken(token: String)
    fun deleteByUserIdAndToken(userId: UUID, token: String)
    fun deleteAllByUserId(userId: UUID)
}

/**
 * Adapter that implements the domain DeviceTokenRepository interface
 * by delegating to the Spring Data JPA repository.
 *
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5
 */
@Repository
open class JpaDeviceTokenRepository(
    private val jpaRepository: SpringDataDeviceTokenRepository
) : DeviceTokenRepository {

    override fun save(deviceToken: DeviceToken): DeviceToken =
        jpaRepository.save(deviceToken)

    override fun findByUserId(userId: UUID): List<DeviceToken> =
        jpaRepository.findByUserId(userId)

    override fun findByToken(token: String): DeviceToken? =
        jpaRepository.findByToken(token)

    override fun deleteByToken(token: String) =
        jpaRepository.deleteByToken(token)

    override fun deleteByUserIdAndToken(userId: UUID, token: String) =
        jpaRepository.deleteByUserIdAndToken(userId, token)

    override fun deleteAllByUserId(userId: UUID) =
        jpaRepository.deleteAllByUserId(userId)
}
