package com.worshiphub.api.scheduling

import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(description = "Service creation response")
data class ScheduleServiceResponse(
    @Schema(description = "Created service event ID")
    val serviceEventId: UUID
)

@Schema(description = "Setlist creation response")
data class SetlistResponse(
    @Schema(description = "Created setlist ID")
    val setlistId: UUID
)

@Schema(description = "Availability marking response")
data class AvailabilityResponse(
    @Schema(description = "Created availability record ID")
    val availabilityId: UUID
)

@Schema(description = "Setlist duration response")
data class SetlistDurationResponse(
    @Schema(description = "Total duration in minutes")
    val durationMinutes: Int
)

@Schema(description = "Service confirmation status")
data class ConfirmationStatusResponse(
    @Schema(description = "User ID")
    val userId: UUID,
    
    @Schema(description = "Assigned role")
    val role: String,
    
    @Schema(description = "Confirmation status")
    val status: String,
    
    @Schema(description = "Response timestamp")
    val respondedAt: String?
)

@Schema(description = "Service event summary")
data class ServiceEventResponse(
    @Schema(description = "Service event ID")
    val id: String,
    
    @Schema(description = "Service name")
    val serviceName: String,
    
    @Schema(description = "Scheduled date")
    val scheduledDate: String,
    
    @Schema(description = "Team ID")
    val teamId: String,
    
    @Schema(description = "Setlist ID")
    val setlistId: String,
    
    @Schema(description = "Service status")
    val status: String
)