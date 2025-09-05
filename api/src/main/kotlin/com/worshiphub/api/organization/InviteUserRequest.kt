package com.worshiphub.api.organization

import com.worshiphub.domain.organization.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Request data for inviting a user to join a church organization")
data class InviteUserRequest(
    @field:NotBlank
    @field:Email
    @Schema(
        description = "User email address for invitation", 
        example = "jane.smith@email.com", 
        required = true
    )
    val email: String,
    
    @field:NotBlank
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Schema(
        description = "User first name", 
        example = "Jane", 
        required = true,
        minLength = 1,
        maxLength = 50
    )
    val firstName: String,
    
    @field:NotBlank
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Schema(
        description = "User last name", 
        example = "Smith", 
        required = true,
        minLength = 1,
        maxLength = 50
    )
    val lastName: String,
    
    @field:NotNull
    @Schema(
        description = "Role to assign to the invited user", 
        example = "TEAM_MEMBER", 
        required = true,
        allowableValues = ["CHURCH_ADMIN", "WORSHIP_LEADER", "TEAM_MEMBER"]
    )
    val role: UserRole
)