package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.scheduling.UserAvailability
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

interface JpaUserAvailabilityRepository : JpaRepository<UserAvailability, UUID> {
    fun findByUserIdAndUnavailableDate(userId: UUID, date: LocalDate): UserAvailability?
    fun findByUserId(userId: UUID): List<UserAvailability>
}

@Repository
open class UserAvailabilityRepositoryImpl(
    private val jpaRepository: JpaUserAvailabilityRepository
) : UserAvailabilityRepository {
    
    override fun save(availability: UserAvailability): UserAvailability = jpaRepository.save(availability)
    
    override fun findById(id: UUID): UserAvailability? = jpaRepository.findById(id).orElse(null)
    
    override fun findByUserIdAndDate(userId: UUID, date: LocalDate): UserAvailability? = 
        jpaRepository.findByUserIdAndUnavailableDate(userId, date)
    
    override fun findByUserId(userId: UUID): List<UserAvailability> = jpaRepository.findByUserId(userId)
    
    override fun delete(availability: UserAvailability) = jpaRepository.delete(availability)
}