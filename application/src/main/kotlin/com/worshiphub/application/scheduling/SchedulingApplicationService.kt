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
@Transactional
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
    fun createSetlist(command: CreateSetlistCommand): Result<UUID> {
        return try {
            val setlist = Setlist(
                name = command.name,
                songIds = command.songIds,
                churchId = command.churchId
            )
            
            // TODO: Persist through repository
            Result.success(setlist.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create setlist", e))
        }
    }
    
    /**
     * Marks a user as unavailable for a specific date.
     */
    fun markUnavailability(command: MarkUnavailabilityCommand): UUID {
        val availability = UserAvailability(
            userId = command.userId,
            unavailableDate = command.unavailableDate,
            reason = command.reason
        )
        
        // TODO: Persist through repository
        return availability.id
    }
    
    /**
     * Gets confirmation status for a service.
     */
    fun getServiceConfirmationStatus(serviceId: UUID): Result<List<AssignedMember>> {
        return try {
            // TODO: Fetch from repository
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get confirmation status", e))
        }
    }
    
    /**
     * Calculates total duration for a setlist.
     */
    fun calculateSetlistDuration(setlistId: UUID): Int {
        // TODO: Fetch setlist and songs from repository
        // TODO: Sum up song durations based on BPM or default values
        return 0 // Placeholder
    }
    
    /**
     * Auto-generates a setlist based on rules.
     */
    fun generateSetlist(command: GenerateSetlistCommand): UUID {
        // TODO: Implement intelligent song selection based on categories
        // TODO: Select songs by category (Opening, Worship, Offering, Closing)
        val generatedSongIds = emptyList<UUID>() // Placeholder
        
        val setlist = Setlist(
            name = command.name,
            songIds = generatedSongIds,
            churchId = command.churchId
        )
        
        // TODO: Persist through repository
        return setlist.id
    }
    
    fun listServiceEvents(churchId: UUID, from: String?, to: String?, page: Int, size: Int): List<Map<String, Any>> {
        // TODO: Implement service listing
        return emptyList()
    }
    
    fun addSongToSetlist(setlistId: UUID, songId: UUID, position: Int) {
        // TODO: Add song to setlist
    }
    
    fun removeSongFromSetlist(setlistId: UUID, songId: UUID) {
        // TODO: Remove song from setlist
    }
    
    fun reorderSetlistSongs(setlistId: UUID, songOrder: List<UUID>) {
        // TODO: Reorder setlist songs
    }
    
    fun getSetlistDetails(setlistId: UUID): Map<String, Any> {
        // TODO: Get setlist details
        return mapOf(
            "id" to setlistId.toString(),
            "name" to "Sample Setlist",
            "songs" to emptyList<String>(),
            "totalDuration" to 0
        )
    }
}