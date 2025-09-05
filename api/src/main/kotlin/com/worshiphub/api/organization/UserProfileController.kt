package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "User Profile", description = "User profile management operations")
@RestController
@RequestMapping("/api/v1/users")
class UserProfileController(
    private val organizationApplicationService: OrganizationApplicationService
) {
    
    @Operation(
        summary = "Get user profile",
        description = "Retrieves the current user's profile information"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @GetMapping("/profile")
    fun getUserProfile(
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): UserProfileResponse {
        val user = organizationApplicationService.getUserProfile(userId)
        return UserProfileResponse(
            id = user["id"]?.toString() ?: "",
            email = user["email"]?.toString() ?: "",
            firstName = user["firstName"]?.toString() ?: "",
            lastName = user["lastName"]?.toString() ?: "",
            role = user["role"]?.toString() ?: "",
            churchId = user["churchId"]?.toString() ?: "",
            isActive = user["isActive"] as? Boolean ?: true,
            createdAt = user["createdAt"]?.toString() ?: "2024-01-01"
        )
    }
    
    @Operation(
        summary = "Update user profile",
        description = "Updates the current user's profile information"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid profile data"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PatchMapping("/profile")
    fun updateUserProfile(
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID,
        @Valid @RequestBody request: UpdateUserProfileRequest
    ): MessageResponse {
        val updates = mutableMapOf<String, String>()
        request.firstName?.let { updates["firstName"] = it }
        request.lastName?.let { updates["lastName"] = it }
        organizationApplicationService.updateUserProfile(userId, updates)
        return MessageResponse("Profile updated successfully")
    }
}