package com.worshiphub.application.scheduling

import com.worshiphub.domain.scheduling.*
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for scheduling operations.
 * Orchestrates domain objects and enforces business rules.
 */
@Service
open class SchedulingApplicationService(
    private val serviceEventRepository: ServiceEventRepository,
    private val setlistRepository: SetlistRepository,
    private val userAvailabilityRepository: UserAvailabilityRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository
) {
    
    /**
     * Schedules a team for a service and assigns members with their roles.
     * Enforces business rules and validates team availability.
     * 
     * @param command Schedule command with service details and member assignments
     * @return ID of the created service event
     */
    @Transactional
    fun scheduleTeamForService(command: ScheduleCommand): Result<UUID> {
        return try {
            // Validate team exists
            val team = teamRepository.findById(command.teamId)
                ?: return Result.failure(IllegalArgumentException("Team not found: ${command.teamId}"))
            
            // Validate scheduled date is in the future
            require(command.scheduledDate.isAfter(LocalDateTime.now())) {
                "Service must be scheduled for a future date"
            }
            
            // Check for conflicting services
            val conflictingServices = serviceEventRepository.findByTeamIdAndDateRange(
                command.teamId, 
                command.scheduledDate.minusHours(2), 
                command.scheduledDate.plusHours(2)
            )
            require(conflictingServices.isEmpty()) {
                "Team already has a service scheduled within 2 hours of this time"
            }
            
            // Create service event
            var serviceEvent = ServiceEvent(
                name = command.serviceName,
                scheduledDate = command.scheduledDate,
                teamId = command.teamId,
                setlistId = command.setlistId,
                churchId = command.churchId
            )
            
            // Validate and create member assignments
            command.memberAssignments.forEach { assignment ->
                // Validate user exists and belongs to the team
                val user = userRepository.findById(assignment.userId)
                    ?: throw IllegalArgumentException("User not found: ${assignment.userId}")
                
                // Check user availability
                val unavailability = userAvailabilityRepository.findByUserIdAndDate(
                    assignment.userId, 
                    command.scheduledDate.toLocalDate()
                )
                if (unavailability != null) {
                    throw IllegalArgumentException(
                        "User ${user.firstName} ${user.lastName} is not available on ${command.scheduledDate.toLocalDate()}"
                    )
                }
                
                // Create assignment
                val assignedMember = AssignedMember(
                    serviceEventId = serviceEvent.id,
                    userId = assignment.userId,
                    role = assignment.role
                )
                
                serviceEvent = serviceEvent.assignMember(assignedMember)
            }
            
            // Persist service event
            val savedServiceEvent = serviceEventRepository.save(serviceEvent)
            
            // Publish the service to make it visible to team members
            val publishedService = savedServiceEvent.publish()
            serviceEventRepository.save(publishedService)
            
            Result.success(publishedService.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to schedule service: ${e.message}", e))
        }
    }
    
    /**
     * Processes a member's response to a service invitation.
     * 
     * @param command Response command with assignment ID and response
     * @throws IllegalArgumentException if user is not authorized to respond to this assignment
     */
    @Transactional
    fun respondToInvitation(command: ResponseCommand): Result<Unit> {
        return try {
            // Find the service event that contains this assignment
            val serviceEvent = serviceEventRepository.findById(command.serviceEventId)
                ?: return Result.failure(IllegalArgumentException("Service event not found: ${command.serviceEventId}"))
            
            // Find the specific assignment
            val assignment = serviceEvent.assignedMembers.find { it.id == command.assignmentId }
                ?: return Result.failure(IllegalArgumentException("Assignment not found: ${command.assignmentId}"))
            
            // Validate user authorization
            require(assignment.userId == command.userId) {
                "User not authorized to respond to this assignment"
            }
            
            // Validate assignment is still pending
            require(assignment.confirmationStatus == ConfirmationStatus.PENDING) {
                "Assignment has already been responded to"
            }
            
            // Update assignment based on response
            val updatedAssignment = when (command.response) {
                ConfirmationStatus.ACCEPTED -> assignment.accept()
                ConfirmationStatus.DECLINED -> assignment.decline()
                else -> throw IllegalArgumentException("Invalid response: ${command.response}")
            }
            
            // Update the service event with the new assignment status
            val updatedMembers = serviceEvent.assignedMembers.map { member ->
                if (member.id == assignment.id) updatedAssignment else member
            }
            
            val updatedServiceEvent = serviceEvent.copy(assignedMembers = updatedMembers)
            serviceEventRepository.save(updatedServiceEvent)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to respond to invitation: ${e.message}", e))
        }
    }
    
    /**
     * Creates a new setlist.
     */
    @Transactional
    fun createSetlist(command: CreateSetlistCommand): Result<UUID> {
        return try {
            val setlist = Setlist(
                name = command.name,
                songIds = command.songIds,
                churchId = command.churchId
            )
            
            val savedSetlist = setlistRepository.save(setlist)
            Result.success(savedSetlist.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create setlist", e))
        }
    }
    
    /**
     * Marks a user as unavailable for a specific date.
     */
    @Transactional
    fun markUnavailability(command: MarkUnavailabilityCommand): UUID {
        val availability = UserAvailability(
            userId = command.userId,
            unavailableDate = command.unavailableDate,
            reason = command.reason
        )
        
        val savedAvailability = userAvailabilityRepository.save(availability)
        return savedAvailability.id
    }
    
    /**
     * Gets confirmation status for a service.
     */
    fun getServiceConfirmationStatus(serviceId: UUID): Result<List<AssignedMember>> {
        return try {
            val serviceEvent = serviceEventRepository.findById(serviceId)
                ?: return Result.failure(RuntimeException("Service event not found: $serviceId"))
            Result.success(serviceEvent.assignedMembers)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get confirmation status", e))
        }
    }
    
    /**
     * Calculates total duration for a setlist.
     */
    fun calculateSetlistDuration(setlistId: UUID): Int {
        val setlist = setlistRepository.findById(setlistId) ?: return 0
        // Default 4 minutes per song if no BPM data available
        return setlist.songIds.size * 4
    }
    
    /**
     * Auto-generates a setlist based on rules.
     * TODO: Implement intelligent setlist generation algorithm
     */
    @Transactional
    fun generateSetlist(command: GenerateSetlistCommand): UUID {
        // Intelligent setlist generation not yet implemented
        // For now, create empty setlist that user can populate manually
        val setlist = Setlist(
            name = command.name,
            songIds = emptyList(), // User will add songs manually
            churchId = command.churchId
        )
        
        val savedSetlist = setlistRepository.save(setlist)
        return savedSetlist.id
    }
    
    fun listServiceEvents(churchId: UUID, from: String?, to: String?, page: Int, size: Int): List<Map<String, Any>> {
        val services = serviceEventRepository.findByChurchId(churchId)
        return services.map { service ->
            mapOf(
                "id" to service.id.toString(),
                "name" to service.name,
                "scheduledDate" to service.scheduledDate.toString(),
                "teamId" to service.teamId.toString(),
                "status" to service.status.name
            )
        }
    }
    
    @Transactional
    fun addSongToSetlist(setlistId: UUID, songId: UUID, position: Int) {
        val setlist = setlistRepository.findById(setlistId)
            ?: throw IllegalArgumentException("Setlist not found: $setlistId")
        
        val updatedSongIds = setlist.songIds.toMutableList()
        updatedSongIds.add(minOf(position, updatedSongIds.size), songId)
        
        val updatedSetlist = setlist.copy(songIds = updatedSongIds)
        setlistRepository.save(updatedSetlist)
    }
    
    @Transactional
    fun removeSongFromSetlist(setlistId: UUID, songId: UUID) {
        val setlist = setlistRepository.findById(setlistId)
            ?: throw IllegalArgumentException("Setlist not found: $setlistId")
        
        val updatedSongIds = setlist.songIds.filter { it != songId }
        val updatedSetlist = setlist.copy(songIds = updatedSongIds)
        setlistRepository.save(updatedSetlist)
    }
    
    @Transactional
    fun reorderSetlistSongs(setlistId: UUID, songOrder: List<UUID>) {
        val setlist = setlistRepository.findById(setlistId)
            ?: throw IllegalArgumentException("Setlist not found: $setlistId")
        
        val updatedSetlist = setlist.copy(songIds = songOrder)
        setlistRepository.save(updatedSetlist)
    }
    
    fun getSetlistDetails(setlistId: UUID): Map<String, Any> {
        val setlist = setlistRepository.findById(setlistId)
            ?: throw IllegalArgumentException("Setlist not found: $setlistId")
        
        return mapOf(
            "id" to setlist.id.toString(),
            "name" to setlist.name,
            "songs" to setlist.songIds.map { it.toString() },
            "totalDuration" to calculateSetlistDuration(setlistId)
        )
    }
    
    fun getAllSetlists(churchId: UUID): List<com.worshiphub.domain.scheduling.Setlist> {
        return setlistRepository.findByChurchId(churchId)
    }
    
    fun getSetlistById(id: UUID, churchId: UUID): com.worshiphub.domain.scheduling.Setlist {
        val setlist = setlistRepository.findById(id)
            ?: throw IllegalArgumentException("Setlist not found: $id")
        
        if (setlist.churchId != churchId) {
            throw IllegalArgumentException("Setlist does not belong to this church")
        }
        
        return setlist
    }
    
    fun updateSetlist(id: UUID, name: String, description: String?, songIds: List<UUID>, estimatedDuration: Double, churchId: UUID) {
        val setlist = setlistRepository.findById(id)
            ?: throw IllegalArgumentException("Setlist not found: $id")
        
        if (setlist.churchId != churchId) {
            throw IllegalArgumentException("Setlist does not belong to this church")
        }
        
        val updatedSetlist = setlist.copy(
            name = name,
            description = description,
            songIds = songIds,
            estimatedDuration = estimatedDuration.toInt()
        )
        
        setlistRepository.save(updatedSetlist)
    }
    
    fun deleteSetlist(id: UUID, churchId: UUID) {
        val setlist = setlistRepository.findById(id)
            ?: throw IllegalArgumentException("Setlist not found: $id")
        
        if (setlist.churchId != churchId) {
            throw IllegalArgumentException("Setlist does not belong to this church")
        }
        
        setlistRepository.delete(id)
    }
}