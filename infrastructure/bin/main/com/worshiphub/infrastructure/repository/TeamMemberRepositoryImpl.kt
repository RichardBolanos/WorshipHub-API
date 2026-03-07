package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.TeamMember
import com.worshiphub.domain.organization.repository.TeamMemberRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaTeamMemberRepository : JpaRepository<TeamMember, UUID> {
    fun findByTeamId(teamId: UUID): List<TeamMember>
    fun findByTeamIdAndUserId(teamId: UUID, userId: UUID): TeamMember?
    fun deleteByTeamIdAndUserId(teamId: UUID, userId: UUID)
}

@Repository
open class TeamMemberRepositoryImpl(
    private val jpaRepository: JpaTeamMemberRepository
) : TeamMemberRepository {
    
    override fun save(teamMember: TeamMember): TeamMember = jpaRepository.save(teamMember)
    override fun findById(id: UUID): TeamMember? = jpaRepository.findById(id).orElse(null)
    override fun findByTeamId(teamId: UUID): List<TeamMember> = jpaRepository.findByTeamId(teamId)
    override fun findByTeamIdAndUserId(teamId: UUID, userId: UUID): TeamMember? = jpaRepository.findByTeamIdAndUserId(teamId, userId)
    override fun deleteByTeamIdAndUserId(teamId: UUID, userId: UUID) = jpaRepository.deleteByTeamIdAndUserId(teamId, userId)
    override fun delete(teamMember: TeamMember) = jpaRepository.delete(teamMember)
}