package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.application.organization.InviteUserCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize
import java.util.*

@Tag(name = "Users", description = "User management and invitation operations")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val organizationApplicationService: OrganizationApplicationService
) {
    
    @Operation(
        summary = "Invite user to church",
        description = "Sends an invitation to a user to join a church organization with a specific role"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User invitation sent successfully"),
        ApiResponse(responseCode = "400", description = "Invalid user data or email format"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "409", description = "User already exists in this church")
    ])
    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('SUPER_ADMIN')")
    fun inviteUser(
        @Valid @RequestBody request: InviteUserRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, UUID> {
        val command = InviteUserCommand(
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            role = request.role,
            churchId = churchId
        )
        
        val userId = organizationApplicationService.inviteUser(command)
        return mapOf("userId" to userId)
    }
}