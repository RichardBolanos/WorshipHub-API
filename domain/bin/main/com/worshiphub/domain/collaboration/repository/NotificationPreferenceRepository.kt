package com.worshiphub.domain.collaboration.repository

import com.worshiphub.domain.collaboration.push.NotificationPreference
import java.util.*

/**
 * Repository interface for managing user notification preferences.
 * Implementations reside in the infrastructure layer (e.g., JpaNotificationPreferenceRepository).
 *
 * Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.6
 */
interface NotificationPreferenceRepository {
    fun save(preference: NotificationPreference): NotificationPreference
    fun findByUserId(userId: UUID): NotificationPreference?
    fun findByUserIdOrDefault(userId: UUID): NotificationPreference
}
