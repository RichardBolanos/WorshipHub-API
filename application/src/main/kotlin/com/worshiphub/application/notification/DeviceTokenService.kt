package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.DevicePlatform
import com.worshiphub.domain.collaboration.push.DeviceToken
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for managing device tokens used in push notifications.
 *
 * Handles registration, deduplication (updating lastUsedAt for existing tokens),
 * and unregistration of FCM device tokens.
 *
 * Validates: Requirements 1.2, 1.3, 1.5, 1.7
 */
@Service
open class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository
) {

    private val logger = LoggerFactory.getLogger(DeviceTokenService::class.java)

    /**
     * Registers a device token for a user. If the token already exists in the database,
     * updates its [DeviceToken.lastUsedAt] timestamp instead of creating a duplicate.
     *
     * @param userId The ID of the user registering the token
     * @param token The FCM device token string
     * @param platform The device platform as a string (ANDROID, IOS, or WEB)
     * @return [Result] containing the token's UUID on success, or a failure with the error
     */
    @Transactional
    open fun registerToken(userId: UUID, token: String, platform: String): Result<UUID> {
        return try {
            val devicePlatform = try {
                DevicePlatform.valueOf(platform.uppercase())
            } catch (e: IllegalArgumentException) {
                return Result.failure(
                    IllegalArgumentException("Unsupported platform: $platform. Must be one of: ANDROID, IOS, WEB")
                )
            }

            // Check if the token already exists — update lastUsedAt instead of creating a duplicate
            val existingToken = deviceTokenRepository.findByToken(token)
            if (existingToken != null) {
                val updatedToken = existingToken.copy(
                    lastUsedAt = LocalDateTime.now(),
                    userId = userId,
                    platform = devicePlatform
                )
                val saved = deviceTokenRepository.save(updatedToken)
                logger.debug("Updated existing device token {} for user {}", token.take(20), userId)
                return Result.success(saved.id)
            }

            // Create a new device token
            val deviceToken = DeviceToken(
                userId = userId,
                token = token,
                platform = devicePlatform
            )
            val saved = deviceTokenRepository.save(deviceToken)
            logger.info("Registered new {} device token for user {}", devicePlatform, userId)
            Result.success(saved.id)
        } catch (e: Exception) {
            logger.error("Failed to register device token for user {}: {}", userId, e.message, e)
            Result.failure(RuntimeException("Failed to register device token", e))
        }
    }

    /**
     * Unregisters a specific device token for a user (e.g., on logout).
     *
     * @param userId The ID of the user
     * @param token The FCM device token to remove
     * @return [Result] indicating success or failure
     */
    @Transactional
    open fun unregisterToken(userId: UUID, token: String): Result<Unit> {
        return try {
            deviceTokenRepository.deleteByUserIdAndToken(userId, token)
            logger.info("Unregistered device token for user {}", userId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to unregister device token for user {}: {}", userId, e.message, e)
            Result.failure(RuntimeException("Failed to unregister device token", e))
        }
    }

    /**
     * Unregisters all device tokens for a user (e.g., on account deletion or full logout).
     *
     * @param userId The ID of the user
     * @return [Result] indicating success or failure
     */
    @Transactional
    open fun unregisterAllTokens(userId: UUID): Result<Unit> {
        return try {
            deviceTokenRepository.deleteAllByUserId(userId)
            logger.info("Unregistered all device tokens for user {}", userId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to unregister all device tokens for user {}: {}", userId, e.message, e)
            Result.failure(RuntimeException("Failed to unregister all device tokens", e))
        }
    }
}
