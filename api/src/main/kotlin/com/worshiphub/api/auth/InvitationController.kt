package com.worshiphub.api.auth

import com.worshiphub.application.auth.*
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@Tag(name = "Invitations", description = "User invitation operations")
@RestController
@RequestMapping("/api/v1/invitations")
class InvitationController(
    private val invitationService: InvitationService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Send invitation",
        description = "Sends an invitation to join the church organization",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Invitation sent successfully"),
        ApiResponse(responseCode = "400", description = "Invalid invitation data"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "409", description = "User already exists")
    ])
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('SUPER_ADMIN')")
    fun sendInvitation(@Valid @RequestBody request: SendInvitationRequest): ResponseEntity<InvitationResponse> {
        val currentUserId = securityContext.getCurrentUserId()
        val churchId = securityContext.getCurrentChurchId()
        
        val command = SendInvitationCommand(
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            churchId = churchId,
            role = request.role,
            invitedBy = currentUserId
        )
        
        return when (val result = invitationService.sendInvitation(command)) {
            is InvitationResult.Success -> 
                ResponseEntity.status(HttpStatus.CREATED).body(
                    InvitationResponse(
                        invitationId = result.id,
                        token = result.token,
                        message = "Invitation sent successfully"
                    )
                )
            is InvitationResult.UserAlreadyExists -> 
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    InvitationResponse(message = "User with this email already exists")
                )
            is InvitationResult.ChurchNotFound -> 
                ResponseEntity.notFound().build()
            is InvitationResult.InviterNotFound -> 
                ResponseEntity.notFound().build()
            is InvitationResult.InsufficientPermissions -> 
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    InvitationResponse(message = "Insufficient permissions to send invitations")
                )
            is InvitationResult.RateLimitExceeded -> 
                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    InvitationResponse(message = "Daily invitation limit exceeded. Try again tomorrow.")
                )
            is InvitationResult.InvalidEmailDomain -> 
                ResponseEntity.badRequest().body(
                    InvitationResponse(message = "Email domain not allowed. Please use a valid email address.")
                )
            else -> ResponseEntity.badRequest().body(
                InvitationResponse(message = "Failed to send invitation")
            )
        }
    }

    @Operation(
        summary = "Get invitation details",
        description = "Retrieves invitation details by token"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Invitation details retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid or expired invitation"),
        ApiResponse(responseCode = "404", description = "Invitation not found")
    ])
    @SecurityRequirements // No security required — public endpoint
    @GetMapping("/{token}")
    fun getInvitationDetails(
        @Parameter(description = "Invitation token", required = true)
        @PathVariable token: String
    ): ResponseEntity<InvitationDetailsResponse> {
        return when (val result = invitationService.getInvitationDetails(token)) {
            is InvitationDetailsResult.Success -> 
                ResponseEntity.ok(
                    InvitationDetailsResponse(
                        email = result.email,
                        firstName = result.firstName,
                        lastName = result.lastName,
                        churchName = result.churchName,
                        role = result.role.name,
                        expiresAt = result.expiresAt.toString()
                    )
                )
            is InvitationDetailsResult.NotFound -> 
                ResponseEntity.notFound().build()
            is InvitationDetailsResult.Expired -> 
                ResponseEntity.badRequest().body(
                    InvitationDetailsResponse(message = "Invitation has expired")
                )
            is InvitationDetailsResult.AlreadyUsed -> 
                ResponseEntity.badRequest().body(
                    InvitationDetailsResponse(message = "Invitation has already been used")
                )
        }
    }

    @Operation(
        summary = "Accept invitation",
        description = "Accepts an invitation and creates user account"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Invitation accepted, user created"),
        ApiResponse(responseCode = "400", description = "Invalid invitation or password"),
        ApiResponse(responseCode = "404", description = "Invitation not found"),
        ApiResponse(responseCode = "409", description = "User already exists")
    ])
    @SecurityRequirements // No security required — public endpoint
    @PostMapping("/{token}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    fun acceptInvitation(
        @Parameter(description = "Invitation token", required = true)
        @PathVariable token: String,
        @Valid @RequestBody request: AcceptInvitationRequest
    ): ResponseEntity<InvitationResponse> {
        return when (val result = invitationService.acceptInvitation(token, request.password)) {
            is InvitationResult.Success -> 
                ResponseEntity.status(HttpStatus.CREATED).body(
                    InvitationResponse(
                        userId = result.id,
                        message = "Invitation accepted successfully. You can now log in."
                    )
                )
            is InvitationResult.InvalidToken -> 
                ResponseEntity.badRequest().body(
                    InvitationResponse(message = "Invalid invitation token")
                )
            is InvitationResult.InvitationExpired -> 
                ResponseEntity.badRequest().body(
                    InvitationResponse(message = "Invitation has expired")
                )
            is InvitationResult.InvitationAlreadyUsed -> 
                ResponseEntity.badRequest().body(
                    InvitationResponse(message = "Invitation has already been used")
                )
            is InvitationResult.UserAlreadyExists -> 
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    InvitationResponse(message = "User with this email already exists")
                )
            is InvitationResult.InvalidPassword -> 
                ResponseEntity.badRequest().body(
                    InvitationResponse(message = "Password requirements not met: ${result.errors.joinToString(", ")}")
                )
            else -> ResponseEntity.badRequest().body(
                InvitationResponse(message = "Failed to accept invitation")
            )
        }
    }
}

data class SendInvitationRequest(
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 50, message = "First name must not exceed 50 characters")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 50, message = "Last name must not exceed 50 characters")
    val lastName: String,
    
    val role: UserRole = UserRole.TEAM_MEMBER
)

data class AcceptInvitationRequest(
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)

data class InvitationResponse(
    val invitationId: UUID? = null,
    val userId: UUID? = null,
    val token: String? = null,
    val message: String
)

data class InvitationDetailsResponse(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val churchName: String? = null,
    val role: String? = null,
    val expiresAt: String? = null,
    val message: String? = null
)