package com.worshiphub.api.auth

import com.worshiphub.api.common.MessageResponse
import com.worshiphub.application.auth.*
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.canManageChurch
import com.worshiphub.domain.organization.canManageTeams
import com.worshiphub.domain.organization.canScheduleServices
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Role Management", description = "User role management operations")
@RestController
@RequestMapping("/api/v1/roles")
class RoleManagementController(
    private val roleManagementService: RoleManagementService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Change user role",
        description = "Changes a user's role within the church organization"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Role changed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid role change request"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('SUPER_ADMIN')")
    fun changeUserRole(
        @Parameter(description = "User ID to change role for", required = true)
        @PathVariable userId: UUID,
        @Valid @RequestBody request: ChangeRoleRequest
    ): ResponseEntity<MessageResponse> {
        val requestingUserId = securityContext.getCurrentUserId()
        
        val command = ChangeUserRoleCommand(
            userId = userId,
            newRole = request.newRole,
            requestedBy = requestingUserId,
            reason = request.reason
        )
        
        return when (val result = roleManagementService.changeUserRole(command)) {
            is RoleChangeResult.Success -> 
                ResponseEntity.ok(MessageResponse("User role changed successfully"))
            is RoleChangeResult.UserNotFound -> 
                ResponseEntity.notFound().build()
            is RoleChangeResult.RequestingUserNotFound -> 
                ResponseEntity.notFound().build()
            is RoleChangeResult.DifferentChurch -> 
                ResponseEntity.badRequest().body(MessageResponse("Cannot change role for users in different churches"))
            is RoleChangeResult.InsufficientPermissions -> 
                ResponseEntity.status(403).body(MessageResponse("Insufficient permissions to change this role"))
            is RoleChangeResult.CannotDemoteSelf -> 
                ResponseEntity.badRequest().body(MessageResponse("Cannot demote yourself from Church Admin role"))
        }
    }

    @Operation(
        summary = "Get church users",
        description = "Retrieves all users in the church with their roles"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Church not found")
    ])
    @GetMapping("/users")
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('SUPER_ADMIN')")
    fun getChurchUsers(): ResponseEntity<ChurchUsersResponse> {
        val requestingUserId = securityContext.getCurrentUserId()
        val churchId = securityContext.getCurrentChurchId()
        
        return when (val result = roleManagementService.getChurchUsers(churchId, requestingUserId)) {
            is ChurchUsersResult.Success -> 
                ResponseEntity.ok(
                    ChurchUsersResponse(
                        users = result.users.map { user ->
                            UserRoleInfo(
                                userId = user.userId,
                                email = user.email,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                role = user.role.name,
                                isActive = user.isActive,
                                isEmailVerified = user.isEmailVerified
                            )
                        }
                    )
                )
            is ChurchUsersResult.RequestingUserNotFound -> 
                ResponseEntity.notFound().build()
            is ChurchUsersResult.DifferentChurch -> 
                ResponseEntity.badRequest().body(ChurchUsersResponse(message = "Access denied"))
            is ChurchUsersResult.InsufficientPermissions -> 
                ResponseEntity.status(403).body(ChurchUsersResponse(message = "Insufficient permissions"))
        }
    }

    @Operation(
        summary = "Get available roles",
        description = "Retrieves all available user roles and their descriptions"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    ])
    @GetMapping("/available")
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('SUPER_ADMIN')")
    fun getAvailableRoles(): ResponseEntity<AvailableRolesResponse> {
        val roles = UserRole.values().filter { it != UserRole.SUPER_ADMIN }.map { role ->
            RoleInfo(
                name = role.name,
                displayName = role.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                description = getRoleDescription(role),
                canManageChurch = role.canManageChurch(),
                canManageTeams = role.canManageTeams(),
                canScheduleServices = role.canScheduleServices()
            )
        }
        
        return ResponseEntity.ok(AvailableRolesResponse(roles))
    }
    
    private fun getRoleDescription(role: UserRole): String = when (role) {
        UserRole.CHURCH_ADMIN -> "Full administrative access to church management"
        UserRole.WORSHIP_LEADER -> "Can manage worship teams and schedule services"
        UserRole.TEAM_MEMBER -> "Can participate in worship teams and view schedules"
        UserRole.SUPER_ADMIN -> "System administrator with global access"
    }
}

data class ChangeRoleRequest(
    val newRole: UserRole,
    
    @field:Size(max = 200, message = "Reason must not exceed 200 characters")
    val reason: String? = null
)

data class UserRoleInfo(
    val userId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val isActive: Boolean,
    val isEmailVerified: Boolean
)

data class RoleInfo(
    val name: String,
    val displayName: String,
    val description: String,
    val canManageChurch: Boolean,
    val canManageTeams: Boolean,
    val canScheduleServices: Boolean
)

data class ChurchUsersResponse(
    val users: List<UserRoleInfo> = emptyList(),
    val message: String? = null
)

data class AvailableRolesResponse(
    val roles: List<RoleInfo>
)