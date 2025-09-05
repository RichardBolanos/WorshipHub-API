package com.worshiphub.api.auth

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

@Schema(description = "User login credentials")
data class LoginRequest(
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    @Schema(description = "User email address", example = "john.doe@church.com", required = true)
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "User password (minimum 6 characters)", example = "mySecurePassword123", required = true)
    val password: String
)

@Schema(description = "Successful login response with JWT token and user information")
data class LoginResponse(
    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,
    
    @Schema(description = "Token type for Authorization header", example = "Bearer")
    val tokenType: String,
    
    @Schema(description = "Token expiration time in seconds", example = "3600")
    val expiresIn: Long,
    
    @Schema(description = "Authenticated user information")
    val user: UserInfo
)

@Schema(description = "User profile information")
data class UserInfo(
    @Schema(description = "Unique user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,
    
    @Schema(description = "User email address", example = "john.doe@church.com")
    val email: String,
    
    @Schema(description = "User first name", example = "John")
    val firstName: String,
    
    @Schema(description = "User last name", example = "Doe")
    val lastName: String,
    
    @Schema(description = "User role in the system", example = "WORSHIP_LEADER", allowableValues = ["CHURCH_ADMIN", "WORSHIP_LEADER", "TEAM_MEMBER"])
    val role: String,
    
    @Schema(description = "Church identifier the user belongs to", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val churchId: UUID
)

@Schema(description = "New user registration data")
data class RegisterRequest(
    @field:NotBlank(message = "First name is required")
    @Schema(description = "User first name", example = "John", required = true)
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @Schema(description = "User last name", example = "Doe", required = true)
    val lastName: String,
    
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    @Schema(description = "User email address (must be unique)", example = "john.doe@church.com", required = true)
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "User password (minimum 6 characters)", example = "mySecurePassword123", required = true)
    val password: String,
    
    @Schema(description = "Church ID to associate user with (optional for new church creation)", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val churchId: UUID?
)

@Schema(description = "Successful user registration response")
data class RegisterResponse(
    @Schema(description = "Newly created user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: UUID,
    
    @Schema(description = "Registration confirmation message", example = "User registered successfully")
    val message: String
)