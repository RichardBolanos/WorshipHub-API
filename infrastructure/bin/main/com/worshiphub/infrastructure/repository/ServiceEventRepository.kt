package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.scheduling.ServiceEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

// @Repository
// interface ServiceEventRepository : JpaRepository<ServiceEvent, UUID> {
//     fun findByTeamId(teamId: UUID): List<ServiceEvent>
// }