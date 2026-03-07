package com.worshiphub.api.scheduling

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Service scheduling response")
data class ScheduleServiceResponse(
    @Schema(description = "Service event ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val serviceId: UUID,
    
    @Schema(description = "Success message", example = "Service scheduled successfully")
    val message: String = "Service scheduled successfully"
)

@Schema(description = "Invitation response result")
data class InvitationResponseResponse(
    @Schema(description = "Response status", example = "ACCEPTED", allowableValues = ["ACCEPTED", "DECLINED"])
    val status: String,
    
    @Schema(description = "Success message", example = "Response recorded successfully")
    val message: String = "Response recorded successfully"
)

@Schema(description = "Setlist creation response")
data class SetlistResponse(
    @Schema(description = "Setlist ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val setlistId: UUID,
    
    @Schema(description = "Success message", example = "Setlist created successfully")
    val message: String = "Setlist created successfully"
)

@Schema(description = "User availability response")
data class AvailabilityResponse(
    @Schema(description = "Availability record ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val availabilityId: UUID,
    
    @Schema(description = "Success message", example = "Unavailability marked successfully")
    val message: String = "Unavailability marked successfully"
)

@Schema(description = "Team member confirmation status")
data class ConfirmationStatusResponse(
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: UUID,
    
    @Schema(description = "Assigned role", example = "Lead Vocalist")
    val role: String,
    
    @Schema(description = "Confirmation status", example = "ACCEPTED", allowableValues = ["PENDING", "ACCEPTED", "DECLINED"])
    val status: String,
    
    @Schema(description = "Response timestamp", example = "2024-01-15T10:30:00")
    val respondedAt: String?
)

@Schema(description = "Setlist duration calculation")
data class SetlistDurationResponse(
    @Schema(description = "Total duration in minutes", example = "45")
    val durationMinutes: Int,
    
    @Schema(description = "Formatted duration", example = "45 minutes")
    val formattedDuration: String = "${durationMinutes} minutes"
)

@Schema(description = "Service event information")
data class ServiceEventResponse(
    @Schema(description = "Service event ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,
    
    @Schema(description = "Service name", example = "Sunday Morning Worship")
    val serviceName: String,
    
    @Schema(description = "Scheduled date", example = "2024-01-21")
    val scheduledDate: String,
    
    @Schema(description = "Team ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val teamId: String,
    
    @Schema(description = "Setlist ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val setlistId: String,
    
    @Schema(description = "Service status", example = "SCHEDULED", allowableValues = ["SCHEDULED", "CONFIRMED", "COMPLETED", "CANCELLED"])
    val status: String
)