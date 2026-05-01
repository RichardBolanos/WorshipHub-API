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
    fun findByUserIdAndUnavailableDateBetween(userId: UUID, startDate: LocalDate, endDate: LocalDate): List<UserAvailability>
    fun findByUserIdInAndUnavailableDate(userIds: List<UUID>, date: LocalDate): List<UserAvailability>
}

@Repository
open class UserAvailabilityRepositoryImpl(
    private val jpaRepository: JpaUserAvailabilityRepository,
    private val jpaTeamMemberRepository: JpaTeamMemberRepository
) : UserAvailabilityRepository {
    
    override fun save(availability: UserAvailability): UserAvailability = jpaRepository.save(availability)
    
    override fun findById(id: UUID): UserAvailability? = jpaRepository.findById(id).orElse(null)
    
    override fun findByUserIdAndDate(userId: UUID, date: LocalDate): UserAvailability? = 
        jpaRepository.findByUserIdAndUnavailableDate(userId, date)
    
    override fun findByUserId(userId: UUID): List<UserAvailability> = jpaRepository.findByUserId(userId)
    
    override fun findByUserIdAndDateRange(userId: UUID, startDate: LocalDate, endDate: LocalDate): List<UserAvailability> =
        jpaRepository.findByUserIdAndUnavailableDateBetween(userId, startDate, endDate)
    
    override fun delete(availability: UserAvailability) = jpaRepository.delete(availability)
    
    override fun deleteByDateAndTeamMembers(date: LocalDate, teamId: UUID) {
        val teamMembers = jpaTeamMemberRepository.findByTeamId(teamId)
        if (teamMembers.isEmpty()) return
        val memberUserIds = teamMembers.map { it.userId }
        val availabilities = jpaRepository.findByUserIdInAndUnavailableDate(memberUserIds, date)
        if (availabilities.isNotEmpty()) {
            jpaRepository.deleteAll(availabilities)
        }
    }
}