package com.worshiphub.api.auth

import com.worshiphub.application.auth.ChurchRegistrationService
import com.worshiphub.application.auth.RegisterChurchWithAdminCommand
import com.worshiphub.application.auth.ChurchRegistrationResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Church Registration", description = "Complete church and admin registration")
@RestController
@RequestMapping("/api/v1/auth/church")
class ChurchRegistrationController(
    private val churchRegistrationService: ChurchRegistrationService
) {

    @Operation(
        summary = "Register new church with admin",
        description = "Creates a new church organization with the first admin user"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Church and admin registered successfully"),
        ApiResponse(responseCode = "400", description = "Invalid registration data"),
        ApiResponse(responseCode = "409", description = "Email already exists")
    ])
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerChurchWithAdmin(@Valid @RequestBody request: RegisterChurchWithAdminRequest): ResponseEntity<ChurchRegistrationResponse> {
        val command = RegisterChurchWithAdminCommand(
            churchName = request.churchName,
            churchAddress = request.churchAddress,
            churchEmail = request.churchEmail,
            adminEmail = request.adminEmail,
            adminFirstName = request.adminFirstName,
            adminLastName = request.adminLastName,
            adminPassword = request.adminPassword
        )
        
        return when (val result = churchRegistrationService.registerChurchWithAdmin(command)) {
            is ChurchRegistrationResult.Success -> 
                ResponseEntity.status(HttpStatus.CREATED).body(
                    ChurchRegistrationResponse(
                        churchId = result.churchId,
                        adminUserId = result.adminUserId,
                        message = "Church and admin registered successfully. Please check email for verification."
                    )
                )
            is ChurchRegistrationResult.EmailAlreadyExists -> 
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ChurchRegistrationResponse(
                        message = "A user with this email already exists"
                    )
                )
            is ChurchRegistrationResult.InvalidPassword -> 
                ResponseEntity.badRequest().body(
                    ChurchRegistrationResponse(
                        message = "Password requirements not met: ${result.errors.joinToString(", ")}"
                    )
                )
        }
    }
}

data class RegisterChurchWithAdminRequest(
    @field:NotBlank(message = "Church name is required")
    @field:Size(max = 100, message = "Church name must not exceed 100 characters")
    val churchName: String,
    
    @field:NotBlank(message = "Church address is required")
    @field:Size(max = 200, message = "Church address must not exceed 200 characters")
    val churchAddress: String,
    
    @field:Email(message = "Invalid church email format")
    @field:NotBlank(message = "Church email is required")
    val churchEmail: String,
    
    @field:Email(message = "Invalid admin email format")
    @field:NotBlank(message = "Admin email is required")
    val adminEmail: String,
    
    @field:NotBlank(message = "Admin first name is required")
    @field:Size(max = 50, message = "First name must not exceed 50 characters")
    val adminFirstName: String,
    
    @field:NotBlank(message = "Admin last name is required")
    @field:Size(max = 50, message = "Last name must not exceed 50 characters")
    val adminLastName: String,
    
    @field:NotBlank(message = "Admin password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val adminPassword: String
)

data class ChurchRegistrationResponse(
    val churchId: UUID? = null,
    val adminUserId: UUID? = null,
    val message: String
)