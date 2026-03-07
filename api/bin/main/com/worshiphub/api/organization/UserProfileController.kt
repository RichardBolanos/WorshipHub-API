package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "User Profile", description = "User profile management operations")
@RestController
@RequestMapping("/api/v1/users")
class UserProfileController(
    private val organizationApplicationService: OrganizationApplicationService,
    private val securityContext: SecurityContext
) {
    
    @Operation(
        summary = "Get user profile",
        description = "Retrieves the current user's profile information",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    fun getUserProfile(): UserProfileResponse {
        val userId = securityContext.getCurrentUserId()
        val user = organizationApplicationService.getUserProfile(userId)
        
        return UserProfileResponse(
            id = userId,
            email = user["email"]?.toString() ?: "",
            firstName = user["firstName"]?.toString() ?: "",
            lastName = user["lastName"]?.toString() ?: "",
            role = user["role"]?.toString() ?: "USER",
            churchId = securityContext.getCurrentChurchId(),
            isEmailVerified = user["isEmailVerified"] as? Boolean ?: false,
            hasPassword = user["hasPassword"] as? Boolean ?: false
        )
    }
    
    @Operation(
        summary = "Update user profile",
        description = "Updates the current user's profile information",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid profile data"),
        ApiResponse(responseCode = "401", description = "User not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    fun updateUserProfile(@Valid @RequestBody request: UpdateUserProfileRequest): MessageResponse {
        val userId = securityContext.getCurrentUserId()
        val updates = mutableMapOf<String, String>()
        request.firstName?.let { updates.put("firstName", it) }
        request.lastName?.let { updates.put("lastName", it) }
        organizationApplicationService.updateUserProfile(userId, updates)
        return MessageResponse("Profile updated successfully")
    }
}

