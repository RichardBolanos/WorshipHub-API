package com.worshiphub.domain.organization.repository

import com.worshiphub.domain.organization.TeamMember
import java.util.*

interface TeamMemberRepository {
    fun save(teamMember: TeamMember): TeamMember
    fun findById(id: UUID): TeamMember?
    fun findByTeamId(teamId: UUID): List<TeamMember>
    fun findByTeamIdAndUserId(teamId: UUID, userId: UUID): TeamMember?
    fun deleteByTeamIdAndUserId(teamId: UUID, userId: UUID)
    fun delete(teamMember: TeamMember)
}