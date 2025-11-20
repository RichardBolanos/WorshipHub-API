package com.worshiphub.api.organization

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Team creation response")
data class CreateTeamResponse(
    @Schema(description = "Team ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val teamId: UUID,
    
    @Schema(description = "Success message", example = "Team created successfully")
    val message: String = "Team created successfully"
)

@Schema(description = "Team information")
data class TeamResponse(
    @Schema(description = "Team ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,
    
    @Schema(description = "Team name", example = "Sunday Morning Worship Team")
    val name: String,
    
    @Schema(description = "Team description", example = "Main worship team for Sunday morning services")
    val description: String?,
    
    @Schema(description = "Team leader ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val leaderId: UUID,
    
    @Schema(description = "Church ID", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val churchId: UUID,
    
    @Schema(description = "Creation timestamp")
    val createdAt: LocalDateTime
)

@Schema(description = "Church information")
data class ChurchResponse(
    @Schema(description = "Church ID", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val id: UUID,
    
    @Schema(description = "Church name", example = "Grace Community Church")
    val name: String,
    
    @Schema(description = "Church address", example = "123 Main Street, Springfield, IL 62701")
    val address: String,
    
    @Schema(description = "Church email", example = "info@gracecommunity.org")
    val email: String,
    
    @Schema(description = "Creation timestamp")
    val createdAt: LocalDateTime
)