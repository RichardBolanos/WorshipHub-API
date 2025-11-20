package com.worshiphub.domain.organization.repository

import com.worshiphub.domain.organization.Team
import java.util.*

/**
 * Repository interface for Team aggregate.
 */
interface TeamRepository {
    
    fun save(team: Team): Team
    fun findById(id: UUID): Team?
    fun findByChurchId(churchId: UUID): List<Team>
    fun findByLeaderId(leaderId: UUID): List<Team>
    fun delete(team: Team)
}