package com.worshiphub.domain.collaboration.push

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Domain entity representing a registered FCM device token for push notifications.
 *
 * Each user can have multiple device tokens (one per active device/browser).
 * Tokens are cleaned up automatically when FCM reports them as invalid.
 *
 * @property id Unique identifier for the device token record
 * @property userId Reference to the user who owns this device
 * @property token The FCM device token string (unique across all users)
 * @property platform The device platform (ANDROID, IOS, or WEB)
 * @property createdAt Timestamp when the token was first registered
 * @property lastUsedAt Timestamp of the last successful use or refresh of this token
 *
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.7, 26.1
 */
@Entity
@Table(name = "device_tokens")
data class DeviceToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 500)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val platform: DevicePlatform,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val lastUsedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Supported device platforms for push notifications.
 */
enum class DevicePlatform {
    ANDROID,
    IOS,
    WEB
}
