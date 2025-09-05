package com.worshiphub.application.auth

import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for role management operations.
 */
@Service
@Transactional
class RoleManagementService(
    private val userRepository: UserRepository,
    private val auditService: AuditService
) {
    
    /**
     * Changes a user's role with proper authorization checks.
     */
    fun changeUserRole(command: ChangeUserRoleCommand): RoleChangeResult {
        val targetUser = userRepository.findById(command.userId)
            ?: return RoleChangeResult.UserNotFound
            
        val requestingUser = userRepository.findById(command.requestedBy)
            ?: return RoleChangeResult.RequestingUserNotFound
            
        // Verify users are in the same church
        if (targetUser.churchId != requestingUser.churchId) {
            return RoleChangeResult.DifferentChurch
        }
        
        // Check authorization
        if (!canChangeRole(requestingUser.role, targetUser.role, command.newRole)) {
            return RoleChangeResult.InsufficientPermissions
        }
        
        // Prevent self-demotion for church admins
        if (command.userId == command.requestedBy && 
            requestingUser.role == UserRole.CHURCH_ADMIN && 
            command.newRole != UserRole.CHURCH_ADMIN) {
            return RoleChangeResult.CannotDemoteSelf
        }
        
        val oldRole = targetUser.role
        val updatedUser = targetUser.copy(role = command.newRole)
        userRepository.save(updatedUser)
        
        // Log the role change
        auditService.logRoleChange(
            userId = command.userId,
            oldRole = oldRole,
            newRole = command.newRole,
            changedBy = command.requestedBy,
            reason = command.reason
        )
        
        return RoleChangeResult.Success
    }
    
    /**
     * Gets all users in a church with their roles.
     */
    fun getChurchUsers(churchId: UUID, requestedBy: UUID): ChurchUsersResult {
        val requestingUser = userRepository.findById(requestedBy)
            ?: return ChurchUsersResult.RequestingUserNotFound
            
        if (requestingUser.churchId != churchId) {
            return ChurchUsersResult.DifferentChurch
        }
        
        if (!canViewChurchUsers(requestingUser.role)) {
            return ChurchUsersResult.InsufficientPermissions
        }
        
        val users = userRepository.findByChurchIdAndIsActiveTrue(churchId)
        val userSummaries = users.map { user ->
            UserRoleSummary(
                userId = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                role = user.role,
                isActive = user.isActive,
                isEmailVerified = user.isEmailVerified
            )
        }
        
        return ChurchUsersResult.Success(userSummaries)
    }
    
    private fun canChangeRole(requestingRole: UserRole, currentRole: UserRole, newRole: UserRole): Boolean {
        return when (requestingRole) {
            UserRole.SUPER_ADMIN -> true // Can change any role
            UserRole.CHURCH_ADMIN -> {
                // Can manage all roles except SUPER_ADMIN
                currentRole != UserRole.SUPER_ADMIN && newRole != UserRole.SUPER_ADMIN
            }
            UserRole.WORSHIP_LEADER -> {
                // Can only promote TEAM_MEMBER to WORSHIP_LEADER or demote WORSHIP_LEADER to TEAM_MEMBER
                (currentRole == UserRole.TEAM_MEMBER && newRole == UserRole.WORSHIP_LEADER) ||
                (currentRole == UserRole.WORSHIP_LEADER && newRole == UserRole.TEAM_MEMBER)
            }
            UserRole.TEAM_MEMBER -> false // Cannot change roles
        }
    }
    
    private fun canViewChurchUsers(role: UserRole): Boolean {
        return role in listOf(UserRole.CHURCH_ADMIN, UserRole.WORSHIP_LEADER, UserRole.SUPER_ADMIN)
    }
}

/**
 * Command for changing user roles.
 */
data class ChangeUserRoleCommand(
    val userId: UUID,
    val newRole: UserRole,
    val requestedBy: UUID,
    val reason: String? = null
)

/**
 * User role summary for listing.
 */
data class UserRoleSummary(
    val userId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val isActive: Boolean,
    val isEmailVerified: Boolean
)

/**
 * Results for role change operations.
 */
sealed class RoleChangeResult {
    object Success : RoleChangeResult()
    object UserNotFound : RoleChangeResult()
    object RequestingUserNotFound : RoleChangeResult()
    object DifferentChurch : RoleChangeResult()
    object InsufficientPermissions : RoleChangeResult()
    object CannotDemoteSelf : RoleChangeResult()
}

/**
 * Results for church users listing.
 */
sealed class ChurchUsersResult {
    data class Success(val users: List<UserRoleSummary>) : ChurchUsersResult()
    object RequestingUserNotFound : ChurchUsersResult()
    object DifferentChurch : ChurchUsersResult()
    object InsufficientPermissions : ChurchUsersResult()
}