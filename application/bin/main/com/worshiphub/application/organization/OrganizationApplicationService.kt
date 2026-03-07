package com.worshiphub.application.organization

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.TeamMember
import com.worshiphub.domain.organization.TeamRole
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.TeamMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for organization operations.
 */
@Service
open class OrganizationApplicationService(
    private val userRepository: UserRepository,
    private val churchRepository: ChurchRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository
) {
    
    /**
     * Gets church details by ID.
     * 
     * @param churchId Church identifier
     * @return Church details
     */
    fun getChurch(churchId: UUID): Result<Church> {
        return try {
            val church = churchRepository.findById(churchId)
                ?: return Result.failure(RuntimeException("Church not found: $churchId"))
            Result.success(church)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Church not found", e))
        }
    }
    
    /**
     * Registers a new church in the platform.
     * 
     * @param command Church registration command
     * @return ID of the created church
     */
    @Transactional
    fun registerChurch(command: RegisterChurchCommand): Result<UUID> {
        return try {
            val church = Church(
                name = command.name,
                address = command.address,
                email = command.email
            )
            
            val savedChurch = churchRepository.save(church)
            Result.success(savedChurch.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to register church", e))
        }
    }
    
    /**
     * Creates a new worship team.
     */
    @Transactional
    fun createTeam(command: CreateTeamCommand): Result<UUID> {
        return try {
            val team = Team(
                name = command.name,
                description = command.description,
                churchId = command.churchId,
                leaderId = command.leaderId
            )
            
            val savedTeam = teamRepository.save(team)
            Result.success(savedTeam.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create team", e))
        }
    }
    
    /**
     * Assigns a member to a team.
     */
    @Transactional
    fun assignTeamMember(command: AssignTeamMemberCommand): Result<UUID> {
        return try {
            val teamMember = TeamMember(
                teamId = command.teamId,
                userId = command.userId,
                teamRole = command.teamRole
            )
            
            val savedTeamMember = teamMemberRepository.save(teamMember)
            Result.success(savedTeamMember.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to assign team member", e))
        }
    }
    
    /**
     * Removes a member from a team.
     */
    @Transactional
    fun removeTeamMember(teamId: UUID, userId: UUID) {
        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId)
    }
    
    /**
     * Gets team members.
     */
    fun getTeamMembers(teamId: UUID): Result<List<TeamMember>> {
        return try {
            val teamMembers = teamMemberRepository.findByTeamId(teamId)
            Result.success(teamMembers)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get team members", e))
        }
    }
    
    /**
     * Invites a user to join a church.
     */
    @Transactional
    fun inviteUser(command: InviteUserCommand): UUID {
        val user = User(
            email = command.email,
            firstName = command.firstName,
            lastName = command.lastName,
            passwordHash = "", // No password for invited users
            churchId = command.churchId,
            role = command.role,
            isActive = false,
            isEmailVerified = false
        )
        
        val savedUser = userRepository.save(user)
        // TODO: Send invitation email
        return savedUser.id
    }
    
    @Transactional
    fun updateTeamMemberRole(teamId: UUID, userId: UUID, role: TeamRole) {
        val teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw IllegalArgumentException("Team member not found")
        
        val updatedTeamMember = teamMember.copy(teamRole = role)
        teamMemberRepository.save(updatedTeamMember)
    }
    
    fun getUserProfile(userId: UUID): Map<String, Any> {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found: $userId")
        
        return mapOf(
            "id" to user.id.toString(),
            "email" to user.email,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "role" to user.role.name,
            "churchId" to user.churchId.toString(),
            "isEmailVerified" to user.isEmailVerified,
            "hasPassword" to (user.passwordHash?.isNotBlank() ?: "")
        )
    }
    
    @Transactional
    fun updateUserProfile(userId: UUID, updates: Map<String, String>) {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found: $userId")
        
        val updatedUser = user.copy(
            firstName = updates["firstName"] ?: user.firstName,
            lastName = updates["lastName"] ?: user.lastName
        )
        
        userRepository.save(updatedUser)
    }
}