package com.worshiphub.api.organization

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "User profile information response")
data class UserProfileResponse(
    @Schema(description = "User unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,
    
    @Schema(description = "User email address", example = "john.doe@church.com")
    val email: String,
    
    @Schema(description = "User first name", example = "John")
    val firstName: String,
    
    @Schema(description = "User last name", example = "Doe")
    val lastName: String,
    
    @Schema(description = "User role in the system", example = "WORSHIP_LEADER")
    val role: String,
    
    @Schema(description = "Church ID the user belongs to", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val churchId: String,
    
    @Schema(description = "Whether the user account is active", example = "true")
    val isActive: Boolean,
    
    @Schema(description = "Account creation timestamp", example = "2024-01-01T10:00:00")
    val createdAt: String
)

@Schema(description = "Request data for updating user profile")
data class UpdateUserProfileRequest(
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Schema(description = "Updated first name", example = "John", minLength = 1, maxLength = 50)
    val firstName: String?,
    
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Schema(description = "Updated last name", example = "Doe", minLength = 1, maxLength = 50)
    val lastName: String?
)

@Schema(description = "Generic message response")
data class MessageResponse(
    @Schema(description = "Response message", example = "Profile updated successfully")
    val message: String
)