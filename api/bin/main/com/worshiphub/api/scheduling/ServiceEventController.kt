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
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import com.worshiphub.api.common.BadRequestException
import com.worshiphub.api.common.NotFoundException
import com.worshiphub.domain.scheduling.ConfirmationStatus
import java.util.*

@Tag(name = "Services & Scheduling", description = "Service scheduling and setlist management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/services")
class ServiceEventController(
    private val schedulingApplicationService: SchedulingApplicationService
) {
    
    @Operation(
        summary = "Schedule a worship service",
        description = "Creates a new service event and assigns team members with their roles",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Service successfully scheduled",
                   content = [Content(schema = Schema(implementation = ScheduleServiceResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid service data"),
        ApiResponse(responseCode = "404", description = "Team or setlist not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
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
        
        val result = schedulingApplicationService.scheduleTeamForService(command)
        return if (result.isSuccess) {
            ScheduleServiceResponse(serviceId = result.getOrThrow())
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to schedule service")
        }
    }
    
    @Operation(
        summary = "Respond to service invitation",
        description = "Allows team members to accept or decline service invitations",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Response recorded successfully",
                   content = [Content(schema = Schema(implementation = InvitationResponseResponse::class))]),
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
            serviceEventId = serviceId,
            assignmentId = assignmentId,
            userId = userId,
            response = ConfirmationStatus.valueOf(request.response)
        )
        
        val result = schedulingApplicationService.respondToInvitation(command)
        return if (result.isSuccess) {
            InvitationResponseResponse(status = request.response)
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to respond to invitation")
        }
    }
    
    @Operation(
        summary = "Create a setlist",
        description = "Creates a new setlist with selected songs for worship services",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Setlist successfully created",
                   content = [Content(schema = Schema(implementation = SetlistResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid setlist data"),
        ApiResponse(responseCode = "404", description = "One or more songs not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PostMapping("/setlists")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createSetlist(
        @Valid @RequestBody request: CreateSetlistRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): SetlistResponse {
        val command = CreateSetlistCommand(
            name = request.name,
            songIds = request.songIds,
            churchId = churchId
        )
        
        val result = schedulingApplicationService.createSetlist(command)
        return if (result.isSuccess) {
            SetlistResponse(setlistId = result.getOrThrow())
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to create setlist")
        }
    }
    
    @Operation(
        summary = "Mark user unavailability",
        description = "Allows team members to mark dates when they are unavailable for services",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Unavailability marked successfully",
                   content = [Content(schema = Schema(implementation = AvailabilityResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid date or request data"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PostMapping("/availability/unavailable")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
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
        return AvailabilityResponse(availabilityId = availabilityId)
    }
    
    @Operation(
        summary = "Get service confirmation status",
        description = "Retrieves the confirmation status of all team members assigned to a service",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Confirmation status retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Service not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @GetMapping("/{serviceId}/confirmations")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getConfirmationStatus(
        @Parameter(description = "Service ID", required = true) @PathVariable serviceId: UUID
    ): List<ConfirmationStatusResponse> {
        val result = schedulingApplicationService.getServiceConfirmationStatus(serviceId)
        val assignments = if (result.isSuccess) {
            result.getOrThrow()
        } else {
            throw NotFoundException(result.exceptionOrNull()?.message ?: "Failed to get confirmation status")
        }
        return assignments.map { assignment ->
            ConfirmationStatusResponse(
                userId = assignment.userId,
                role = assignment.role,
                status = assignment.confirmationStatus.name,
                respondedAt = assignment.respondedAt?.toString()
            )
        }
    }
    
    @Operation(
        summary = "Calculate setlist duration",
        description = "Calculates the total estimated duration of a setlist in minutes",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Duration calculated successfully",
                   content = [Content(schema = Schema(implementation = SetlistDurationResponse::class))]),
        ApiResponse(responseCode = "404", description = "Setlist not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @GetMapping("/setlists/{setlistId}/duration")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getSetlistDuration(
        @Parameter(description = "Setlist ID", required = true) @PathVariable setlistId: UUID
    ): SetlistDurationResponse {
        val duration = schedulingApplicationService.calculateSetlistDuration(setlistId)
        return SetlistDurationResponse(durationMinutes = duration)
    }
    
    @Operation(
        summary = "Auto-generate setlist",
        description = "Automatically generates a setlist based on predefined rules and categories",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Setlist successfully generated",
                   content = [Content(schema = Schema(implementation = SetlistResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid generation rules"),
        ApiResponse(responseCode = "404", description = "Insufficient songs for generation"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
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
        return SetlistResponse(setlistId = setlistId)
    }
    
    @Operation(
        summary = "List service events",
        description = "Retrieves scheduled service events for a church within a date range",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Service events retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @GetMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
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