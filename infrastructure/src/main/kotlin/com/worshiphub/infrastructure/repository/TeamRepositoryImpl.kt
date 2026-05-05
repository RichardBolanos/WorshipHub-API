package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.repository.TeamRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaTeamRepository : JpaRepository<Team, UUID> {
    fun findByChurchId(churchId: UUID): List<Team>
    fun findByLeaderId(leaderId: UUID): List<Team>
}

@Repository
open class TeamRepositoryImpl(
    private val jpaRepository: JpaTeamRepository,
    private val entityManager: EntityManager
) : TeamRepository {
    
    override fun save(team: Team): Team {
        return if (jpaRepository.existsById(team.id)) {
            jpaRepository.save(team)
        } else {
            entityManager.persist(team)
            team
        }
    }
    
    override fun findById(id: UUID): Team? = jpaRepository.findById(id).orElse(null)
    
    override fun findByChurchId(churchId: UUID): List<Team> = jpaRepository.findByChurchId(churchId)
    
    override fun findByLeaderId(leaderId: UUID): List<Team> = jpaRepository.findByLeaderId(leaderId)
    
    override fun delete(team: Team) = jpaRepository.delete(team)
}