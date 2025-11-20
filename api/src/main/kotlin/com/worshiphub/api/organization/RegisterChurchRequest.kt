package com.worshiphub.api.organization

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*

@Schema(description = "Request data for registering a new church organization")
data class RegisterChurchRequest(
    @field:NotBlank(message = "Church name is required")
    @field:Size(min = 2, max = 200, message = "Church name must be between 2 and 200 characters")
    @field:Pattern(regexp = "^[a-zA-Z0-9\\s\\-'.,()]+$", 
                  message = "Church name contains invalid characters")
    @Schema(
        description = "Official name of the church",
        example = "Grace Community Church",
        required = true,
        minLength = 2,
        maxLength = 200
    )
    val name: String,
    
    @field:NotBlank(message = "Address is required")
    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    @Schema(
        description = "Physical address of the church",
        example = "123 Main Street, Springfield, IL 62701",
        required = true,
        minLength = 10,
        maxLength = 500
    )
    val address: String,
    
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 100, message = "Email too long")
    @Schema(
        description = "Primary contact email for the church",
        example = "contact@gracechurch.org",
        required = true,
        format = "email",
        maxLength = 100
    )
    val email: String
)