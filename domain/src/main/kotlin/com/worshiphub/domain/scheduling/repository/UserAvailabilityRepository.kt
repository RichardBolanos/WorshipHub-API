package com.worshiphub.domain.scheduling.repository

import com.worshiphub.domain.scheduling.UserAvailability
import java.time.LocalDate
import java.util.*

/**
 * Repository interface for UserAvailability entity.
 */
interface UserAvailabilityRepository {
    
    fun save(availability: UserAvailability): UserAvailability
    fun findById(id: UUID): UserAvailability?
    fun findByUserIdAndDate(userId: UUID, date: LocalDate): UserAvailability?
    fun findByUserId(userId: UUID): List<UserAvailability>
    fun delete(availability: UserAvailability)
}