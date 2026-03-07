package com.worshiphub.api.scheduling

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Request data for scheduling a worship service with team assignments")
data class ScheduleServiceRequest(
    @field:NotBlank
    @Schema(
        description = "Name of the service event", 
        example = "Sunday Morning Worship", 
        required = true
    )
    val serviceName: String,
    
    @field:NotNull
    @Schema(
        description = "Date and time when the service is scheduled", 
        example = "2024-01-07T10:00:00", 
        required = true
    )
    val scheduledDate: LocalDateTime,
    
    @field:NotNull
    @Schema(
        description = "ID of the worship team to schedule", 
        example = "123e4567-e89b-12d3-a456-426614174000", 
        required = true
    )
    val teamId: UUID,
    
    @Schema(
        description = "Optional setlist ID for the service", 
        example = "987fcdeb-51a2-43d1-9c4e-123456789abc"
    )
    val setlistId: UUID? = null,
    
    @field:NotEmpty
    @field:Valid
    @Schema(
        description = "List of team member assignments with their roles", 
        required = true
    )
    val memberAssignments: List<MemberAssignmentRequest>
)

@Schema(description = "Team member assignment with specific role for a service")
data class MemberAssignmentRequest(
    @field:NotNull
    @Schema(
        description = "ID of the team member to assign", 
        example = "456e7890-e89b-12d3-a456-426614174111", 
        required = true
    )
    val userId: UUID,
    
    @field:NotBlank
    @Schema(
        description = "Role assignment for this service", 
        example = "Lead Vocalist", 
        required = true,
        allowableValues = ["Lead Vocalist", "Backup Vocalist", "Acoustic Guitar", "Electric Guitar", "Bass", "Drums", "Keys", "Sound Engineer"]
    )
    val role: String
)