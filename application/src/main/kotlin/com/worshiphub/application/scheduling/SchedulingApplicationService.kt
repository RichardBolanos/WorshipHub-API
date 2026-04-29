package com.worshiphub.application.scheduling

import com.worshiphub.domain.scheduling.*
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
     * Creates a recurring service event and generates all child instances
     * based on the recurrence rule.
     *
     * Business rules:
     * - End date must be after start date
     * - Max horizon is 52 weeks if no end date specified
     * - MONTHLY with day 31 uses last day of shorter months
     * - Each generated instance gets parentServiceId = parent's ID
     */
    @Transactional
    open fun createRecurringService(command: CreateRecurringServiceCommand): Result<UUID> {
        return try {
            // Validate team exists
            teamRepository.findById(command.teamId)
                ?: return Result.failure(IllegalArgumentException("Team not found: ${command.teamId}"))

            // Validate end date is after start date
            val endDate = command.recurrenceRule.recurrenceEndDate
            if (endDate != null && !endDate.isAfter(command.scheduledDate.toLocalDate())) {
                return Result.failure(
                    IllegalArgumentException(
                        "La fecha de fin de recurrencia debe ser posterior a la fecha del culto"
                    )
                )
            }

            // Create parent service event with recurrence rule
            val parentService = ServiceEvent(
                name = command.serviceName,
                scheduledDate = command.scheduledDate,
                teamId = command.teamId,
                churchId = command.churchId,
                recurrenceRule = command.recurrenceRule
            )
            val savedParent = serviceEventRepository.save(parentService)

            // Generate child instances
            val dates = generateRecurrenceDates(
                startDate = command.scheduledDate,
                rule = command.recurrenceRule
            )

            for (date in dates) {
                val childService = ServiceEvent(
                    name = command.serviceName,
                    scheduledDate = date,
                    teamId = command.teamId,
                    churchId = command.churchId,
                    parentServiceId = savedParent.id
                )
                serviceEventRepository.save(childService)
            }

            Result.success(savedParent.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create recurring service: ${e.message}", e))
        }
    }

    /**
     * Updates the recurrence rule for an existing recurring service.
     * Only regenerates future instances that don't have ACCEPTED members.
     * Instances with ACCEPTED members are preserved.
     */
    @Transactional
    open fun updateRecurrenceRule(serviceId: UUID, newRule: RecurrenceRule): Result<Unit> {
        return try {
            val parentService = serviceEventRepository.findById(serviceId)
                ?: return Result.failure(IllegalArgumentException("Service event not found: $serviceId"))

            // Update parent with new rule
            val updatedParent = parentService.copy(recurrenceRule = newRule)
            serviceEventRepository.save(updatedParent)

            // Get existing child instances
            val children = serviceEventRepository.findByParentServiceId(serviceId)

            // Separate: keep instances with ACCEPTED members, delete the rest that are in the future
            val now = LocalDateTime.now()
            val toDelete = children.filter { child ->
                child.scheduledDate.isAfter(now) &&
                    child.assignedMembers.none { it.confirmationStatus == ConfirmationStatus.ACCEPTED }
            }
            if (toDelete.isNotEmpty()) {
                serviceEventRepository.deleteAll(toDelete)
            }

            // Generate new future instances based on new rule
            val dates = generateRecurrenceDates(
                startDate = parentService.scheduledDate,
                rule = newRule
            )

            // Only create instances for dates that are in the future and not already covered
            // by preserved (ACCEPTED) instances
            val preservedDates = children
                .filter { child ->
                    child.scheduledDate.isAfter(now) &&
                        child.assignedMembers.any { it.confirmationStatus == ConfirmationStatus.ACCEPTED }
                }
                .map { it.scheduledDate }
                .toSet()

            for (date in dates) {
                if (date.isAfter(now) && date !in preservedDates) {
                    val childService = ServiceEvent(
                        name = parentService.name,
                        scheduledDate = date,
                        teamId = parentService.teamId,
                        churchId = parentService.churchId,
                        parentServiceId = serviceId
                    )
                    serviceEventRepository.save(childService)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to update recurrence rule: ${e.message}", e))
        }
    }

    /**
     * Deletes a recurring service and its child instances.
     * Only deletes instances in DRAFT or PUBLISHED status without ACCEPTED members.
     */
    @Transactional
    open fun deleteRecurringService(serviceId: UUID): Result<Unit> {
        return try {
            val parentService = serviceEventRepository.findById(serviceId)
                ?: return Result.failure(IllegalArgumentException("Service event not found: $serviceId"))

            val children = serviceEventRepository.findByParentServiceId(serviceId)

            // Delete child instances that are DRAFT/PUBLISHED and have no ACCEPTED members
            val deletable = children.filter { child ->
                (child.status == ServiceEventStatus.DRAFT || child.status == ServiceEventStatus.PUBLISHED) &&
                    child.assignedMembers.none { it.confirmationStatus == ConfirmationStatus.ACCEPTED }
            }

            if (deletable.isNotEmpty()) {
                // Cascade-delete availability records for each deleted child
                for (child in deletable) {
                    userAvailabilityRepository.deleteByDateAndTeamMembers(
                        child.scheduledDate.toLocalDate(),
                        child.teamId
                    )
                }
                serviceEventRepository.deleteAll(deletable)
            }

            // Delete parent if it also qualifies
            if ((parentService.status == ServiceEventStatus.DRAFT || parentService.status == ServiceEventStatus.PUBLISHED) &&
                parentService.assignedMembers.none { it.confirmationStatus == ConfirmationStatus.ACCEPTED }
            ) {
                userAvailabilityRepository.deleteByDateAndTeamMembers(
                    parentService.scheduledDate.toLocalDate(),
                    parentService.teamId
                )
                serviceEventRepository.delete(parentService)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to delete recurring service: ${e.message}", e))
        }
    }

    /**
     * Deletes an availability (unavailability) record.
     * Returns 404 if not found, 403 if userId doesn't match.
     */
    @Transactional
    open fun deleteAvailability(command: DeleteAvailabilityCommand): Result<Unit> {
        val availability = userAvailabilityRepository.findById(command.availabilityId)
            ?: return Result.failure(NoSuchElementException("Registro de indisponibilidad no encontrado"))

        if (availability.userId != command.userId) {
            return Result.failure(SecurityException("No tiene permiso para eliminar este registro"))
        }

        userAvailabilityRepository.delete(availability)
        return Result.success(Unit)
    }

    /**
     * Gets the current user's availability records, optionally filtered by date range,
     * ordered by date ascending.
     */
    open fun getMyAvailability(command: GetMyAvailabilityCommand): List<UserAvailability> {
        val records = if (command.startDate != null && command.endDate != null) {
            userAvailabilityRepository.findByUserIdAndDateRange(
                command.userId,
                command.startDate,
                command.endDate
            )
        } else {
            userAvailabilityRepository.findByUserId(command.userId)
        }
        return records.sortedBy { it.unavailableDate }
    }

    /**
     * Generates recurrence dates based on a start date and recurrence rule.
     * Skips the first occurrence (the parent date) and generates subsequent dates.
     * For MONTHLY with day > last day of month, uses last day of that month.
     * Max horizon: recurrenceEndDate or 52 weeks from start.
     */
    internal fun generateRecurrenceDates(
        startDate: LocalDateTime,
        rule: RecurrenceRule
    ): List<LocalDateTime> {
        val dates = mutableListOf<LocalDateTime>()
        val maxEnd = rule.recurrenceEndDate
            ?: startDate.toLocalDate().plusWeeks(52)
        val originalDay = startDate.dayOfMonth

        var current = advanceDate(startDate, rule.frequency, originalDay)

        while (!current.toLocalDate().isAfter(maxEnd)) {
            dates.add(current)
            current = advanceDate(current, rule.frequency, originalDay)
        }

        return dates
    }

    private fun advanceDate(
        date: LocalDateTime,
        frequency: RecurrenceFrequency,
        originalDay: Int
    ): LocalDateTime {
        return when (frequency) {
            RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> {
                val nextMonth = date.toLocalDate().plusMonths(1)
                val yearMonth = YearMonth.of(nextMonth.year, nextMonth.month)
                val day = minOf(originalDay, yearMonth.lengthOfMonth())
                LocalDateTime.of(
                    yearMonth.atDay(day),
                    date.toLocalTime()
                )
            }
            RecurrenceFrequency.YEARLY -> date.plusYears(1)
        }
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
                "serviceName" to service.name,
                "scheduledDate" to service.scheduledDate.toString(),
                "teamId" to service.teamId.toString(),
                "setlistId" to (service.setlistId?.toString() ?: ""),
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
        
        // Validate duration (max 90 minutes)
        require(estimatedDuration <= 90) {
            "Setlist duration cannot exceed 90 minutes"
        }
        
        val updatedSetlist = setlist.copy(
            name = name,
            description = description,
            songIds = songIds,
            estimatedDuration = estimatedDuration.toInt(),
            updatedAt = LocalDateTime.now()
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