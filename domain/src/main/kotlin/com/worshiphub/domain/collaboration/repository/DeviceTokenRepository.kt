package com.worshiphub.domain.collaboration.repository

import com.worshiphub.domain.collaboration.push.DeviceToken
import java.util.*

/**
 * Repository interface for managing device tokens used in push notifications.
 * Implementations reside in the infrastructure layer (e.g., JpaDeviceTokenRepository).
 *
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.7, 26.1
 */
interface DeviceTokenRepository {
    fun save(deviceToken: DeviceToken): DeviceToken
    fun findByUserId(userId: UUID): List<DeviceToken>
    fun findByToken(token: String): DeviceToken?
    fun deleteByToken(token: String)
    fun deleteByUserIdAndToken(userId: UUID, token: String)
    fun deleteAllByUserId(userId: UUID)
}
