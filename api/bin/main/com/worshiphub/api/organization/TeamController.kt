package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.application.organization.CreateTeamCommand
import com.worshiphub.application.organization.AssignTeamMemberCommand
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
import java.util.*

@Tag(name = "Teams", description = "Worship team management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/teams")
class TeamController(
    private val organizationApplicationService: OrganizationApplicationService
) {
    
    @Operation(
        summary = "Create a new worship team",
        description = "Creates a new worship team within a church organization",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Team successfully created",
                   content = [Content(schema = Schema(implementation = CreateTeamResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasAuthority('CHURCH_ADMIN') or hasAuthority('WORSHIP_LEADER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(
        @Valid @RequestBody request: CreateTeamRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): CreateTeamResponse {
        val command = CreateTeamCommand(
            name = request.name,
            description = request.description,
            churchId = churchId,
            leaderId = request.leaderId
        )
        
        val result = organizationApplicationService.createTeam(command)
        return if (result.isSuccess) {
            CreateTeamResponse(teamId = result.getOrThrow())
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to create team")
        }
    }
}