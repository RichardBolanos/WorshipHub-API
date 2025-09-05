package com.worshiphub.api.scheduling

import com.worshiphub.application.scheduling.SchedulingApplicationService
import com.worshiphub.application.scheduling.ScheduleCommand
import com.worshiphub.application.scheduling.MemberAssignment
import com.worshiphub.application.scheduling.ResponseCommand
import com.worshiphub.application.scheduling.CreateSetlistCommand
import com.worshiphub.application.scheduling.MarkUnavailabilityCommand
import com.worshiphub.application.scheduling.GenerateSetlistCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Services & Scheduling", description = "Service scheduling and setlist management operations")
@RestController
@RequestMapping("/api/v1/services")
class ServiceEventController(
    private val schedulingApplicationService: SchedulingApplicationService
) {
    
    @Operation(
        summary = "Schedule a worship service",
        description = "Creates a new service event and assigns team members with their roles"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Service successfully scheduled"),
        ApiResponse(responseCode = "400", description = "Invalid service data"),
        ApiResponse(responseCode = "404", description = "Team or setlist not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun scheduleService(
        @Valid @RequestBody request: ScheduleServiceRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): ScheduleServiceResponse {
        
        val command = ScheduleCommand(
            serviceName = request.serviceName,
            scheduledDate = request.scheduledDate,
            teamId = request.teamId,
            setlistId = request.setlistId,
            memberAssignments = request.memberAssignments.map { 
                MemberAssignment(it.userId, it.role) 
            },
            churchId = churchId
        )
        
        val serviceEventId = schedulingApplicationService.scheduleTeamForService(command)
        
        return ScheduleServiceResponse(serviceEventId)
    }
    
    @Operation(
        summary = "Respond to service invitation",
        description = "Allows team members to accept or decline service invitations"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Response recorded successfully"),
        ApiResponse(responseCode = "400", description = "Invalid response data"),
        ApiResponse(responseCode = "404", description = "Service or assignment not found"),
        ApiResponse(responseCode = "403", description = "User not authorized to respond to this assignment")
    ])
    @PatchMapping("/{serviceId}/assignments/{assignmentId}")
    fun respondToInvitation(
        @Parameter(description = "Service ID", required = true) @PathVariable serviceId: UUID,
        @Parameter(description = "Assignment ID", required = true) @PathVariable assignmentId: UUID,
        @Valid @RequestBody request: InvitationResponseRequest,
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): InvitationResponseResponse {
        
        val command = ResponseCommand(
            assignmentId = assignmentId,
            userId = userId,
            response = request.response
        )
        
        schedulingApplicationService.respondToInvitation(command)
        
        return InvitationResponseResponse(request.response)
    }
    
    @Operation(
        summary = "Create a setlist",
        description = "Creates a new setlist with selected songs for worship services"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Setlist successfully created"),
        ApiResponse(responseCode = "400", description = "Invalid setlist data"),
        ApiResponse(responseCode = "404", description = "One or more songs not found")
    ])
    @PostMapping("/setlists")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSetlist(
        @Valid @RequestBody request: CreateSetlistRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): SetlistResponse {
        val command = CreateSetlistCommand(
            name = request.name,
            songIds = request.songIds,
            churchId = churchId
        )
        
        val setlistId = schedulingApplicationService.createSetlist(command)
        return SetlistResponse(setlistId)
    }
    
    @Operation(
        summary = "Mark user unavailability",
        description = "Allows team members to mark dates when they are unavailable for services"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Unavailability marked successfully"),
        ApiResponse(responseCode = "400", description = "Invalid date or request data"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PostMapping("/availability/unavailable")
    @ResponseStatus(HttpStatus.CREATED)
    fun markUnavailability(
        @Valid @RequestBody request: MarkUnavailabilityRequest,
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): AvailabilityResponse {
        val command = MarkUnavailabilityCommand(
            userId = userId,
            unavailableDate = request.unavailableDate,
            reason = request.reason
        )
        
        val availabilityId = schedulingApplicationService.markUnavailability(command)
        return AvailabilityResponse(availabilityId)
    }
    
    @Operation(
        summary = "Get service confirmation status",
        description = "Retrieves the confirmation status of all team members assigned to a service"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Confirmation status retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Service not found")
    ])
    @GetMapping("/{serviceId}/confirmations")
    fun getConfirmationStatus(
        @Parameter(description = "Service ID", required = true) @PathVariable serviceId: UUID
    ): List<ConfirmationStatusResponse> {
        val assignments = schedulingApplicationService.getServiceConfirmationStatus(serviceId)
        return assignments.map { assignment ->
            ConfirmationStatusResponse(
                userId = assignment.userId,
                role = assignment.role,
                status = assignment.confirmationStatus.toString(),
                respondedAt = assignment.respondedAt?.toString()
            )
        }
    }
    
    @Operation(
        summary = "Calculate setlist duration",
        description = "Calculates the total estimated duration of a setlist in minutes"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Duration calculated successfully"),
        ApiResponse(responseCode = "404", description = "Setlist not found")
    ])
    @GetMapping("/setlists/{setlistId}/duration")
    fun getSetlistDuration(
        @Parameter(description = "Setlist ID", required = true) @PathVariable setlistId: UUID
    ): SetlistDurationResponse {
        val duration = schedulingApplicationService.calculateSetlistDuration(setlistId)
        return SetlistDurationResponse(duration)
    }
    
    @Operation(
        summary = "Auto-generate setlist",
        description = "Automatically generates a setlist based on predefined rules and categories"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Setlist successfully generated"),
        ApiResponse(responseCode = "400", description = "Invalid generation rules"),
        ApiResponse(responseCode = "404", description = "Insufficient songs for generation"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PostMapping("/setlists/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateSetlist(
        @Valid @RequestBody request: GenerateSetlistRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): SetlistResponse {
        val command = GenerateSetlistCommand(
            name = request.name,
            churchId = churchId,
            rules = request.rules
        )
        
        val setlistId = schedulingApplicationService.generateSetlist(command)
        return SetlistResponse(setlistId)
    }
    
    @Operation(
        summary = "List service events",
        description = "Retrieves scheduled service events for a church within a date range"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Service events retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Church not found")
    ])
    @GetMapping
    fun listServiceEvents(
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID,
        @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) from: String?,
        @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) to: String?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): List<ServiceEventResponse> {
        val events = schedulingApplicationService.listServiceEvents(churchId, from, to, page, size)
        return events.map { event ->
            ServiceEventResponse(
                id = event["id"]?.toString() ?: "",
                serviceName = event["serviceName"]?.toString() ?: "",
                scheduledDate = event["scheduledDate"]?.toString() ?: "",
                teamId = event["teamId"]?.toString() ?: "",
                setlistId = event["setlistId"]?.toString() ?: "",
                status = event["status"]?.toString() ?: "SCHEDULED"
            )
        }
    }
}