package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.push.NotificationPreference
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Spring Data JPA interface for NotificationPreference persistence operations.
 */
@Repository
interface SpringDataNotificationPreferenceRepository : JpaRepository<NotificationPreference, UUID> {
    fun findByUserId(userId: UUID): NotificationPreference?
}

/**
 * Adapter that implements the domain NotificationPreferenceRepository interface
 * by delegating to the Spring Data JPA repository.
 *
 * The [findByUserIdOrDefault] method returns a new [NotificationPreference] instance
 * with all boolean fields set to `true` (the data class defaults) when no record
 * exists for the given user, satisfying the "all enabled by default" requirement.
 *
 * Validates: Requirements 11.2, 11.4
 */
@Repository
open class JpaNotificationPreferenceRepository(
    private val jpaRepository: SpringDataNotificationPreferenceRepository
) : NotificationPreferenceRepository {

    override fun save(preference: NotificationPreference): NotificationPreference =
        jpaRepository.save(preference)

    override fun findByUserId(userId: UUID): NotificationPreference? =
        jpaRepository.findByUserId(userId)

    override fun findByUserIdOrDefault(userId: UUID): NotificationPreference =
        jpaRepository.findByUserId(userId)
            ?: NotificationPreference(userId = userId)
}
