package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.scheduling.ServiceEvent
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

interface JpaServiceEventRepository : JpaRepository<ServiceEvent, UUID> {
    
    @Query("SELECT s FROM ServiceEvent s WHERE s.teamId = :teamId AND s.scheduledDate BETWEEN :startDate AND :endDate")
    fun findByTeamIdAndDateRange(teamId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): List<ServiceEvent>
    
    fun findByChurchId(churchId: UUID): List<ServiceEvent>
    
    @Query("SELECT s FROM ServiceEvent s WHERE s.teamId = :teamId AND s.scheduledDate > CURRENT_TIMESTAMP ORDER BY s.scheduledDate")
    fun findUpcomingByTeamId(teamId: UUID): List<ServiceEvent>
    
    fun findByParentServiceId(parentServiceId: UUID): List<ServiceEvent>
}

@Repository
open class ServiceEventRepositoryImpl(
    private val jpaRepository: JpaServiceEventRepository
) : ServiceEventRepository {
    
    override fun save(serviceEvent: ServiceEvent): ServiceEvent = jpaRepository.save(serviceEvent)
    
    override fun findById(id: UUID): ServiceEvent? = jpaRepository.findById(id).orElse(null)
    
    override fun findByTeamIdAndDateRange(teamId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): List<ServiceEvent> =
        jpaRepository.findByTeamIdAndDateRange(teamId, startDate, endDate)
    
    override fun findByChurchId(churchId: UUID): List<ServiceEvent> = jpaRepository.findByChurchId(churchId)
    
    override fun findUpcomingByTeamId(teamId: UUID): List<ServiceEvent> = jpaRepository.findUpcomingByTeamId(teamId)
    
    override fun findByParentServiceId(parentServiceId: UUID): List<ServiceEvent> = 
        jpaRepository.findByParentServiceId(parentServiceId)
    
    override fun delete(serviceEvent: ServiceEvent) = jpaRepository.delete(serviceEvent)
    
    override fun deleteAll(serviceEvents: List<ServiceEvent>) = jpaRepository.deleteAll(serviceEvents)
}