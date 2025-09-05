package com.worshiphub.api.organization

import jakarta.validation.constraints.*

/**
 * Request DTO for church registration with comprehensive validation.
 */
data class RegisterChurchRequest(
    @field:NotBlank(message = "Church name is required")
    @field:Size(min = 2, max = 200, message = "Church name must be between 2 and 200 characters")
    @field:Pattern(regexp = "^[a-zA-Z0-9\\s\\-'.,()]+$", 
                  message = "Church name contains invalid characters")
    val name: String,
    
    @field:NotBlank(message = "Address is required")
    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    val address: String,
    
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 100, message = "Email too long")
    val email: String
)