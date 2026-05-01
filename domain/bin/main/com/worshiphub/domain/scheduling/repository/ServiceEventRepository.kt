package com.worshiphub.domain.scheduling.repository

import com.worshiphub.domain.scheduling.ServiceEvent
import java.time.LocalDateTime
import java.util.*

/**
 * Repository interface for ServiceEvent aggregate.
 */
interface ServiceEventRepository {
    
    fun save(serviceEvent: ServiceEvent): ServiceEvent
    fun findById(id: UUID): ServiceEvent?
    fun findByTeamIdAndDateRange(teamId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): List<ServiceEvent>
    fun findByChurchId(churchId: UUID): List<ServiceEvent>
    fun findUpcomingByTeamId(teamId: UUID): List<ServiceEvent>
    fun findByParentServiceId(parentServiceId: UUID): List<ServiceEvent>
    fun delete(serviceEvent: ServiceEvent)
    fun deleteAll(serviceEvents: List<ServiceEvent>)
}