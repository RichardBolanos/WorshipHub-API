package com.worshiphub.api.auth

import com.worshiphub.application.auth.AuthenticationService
import com.worshiphub.application.auth.AuthenticationResult
import com.worshiphub.application.auth.RegisterUserCommand
import com.worshiphub.application.auth.RegisterResult
import com.worshiphub.security.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Authentication", description = "User authentication and authorization operations")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticationService: AuthenticationService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Operation(
        summary = "User login",
        description = "Authenticates user and returns JWT token for API access"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Login successful"),
        ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ApiResponse(responseCode = "403", description = "Email verification required or account inactive"),
        ApiResponse(responseCode = "400", description = "Invalid request data")
    ])
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<*> {
        return when (val result = authenticationService.authenticate(request.email, request.password)) {
            is AuthenticationResult.Success -> {
                val token = jwtTokenProvider.generateToken(
                    userId = result.user.id.toString(),
                    churchId = result.user.churchId.toString(),
                    roles = listOf(result.user.role.name)
                )
                ResponseEntity.ok(LoginResponse(
                    token = token,
                    tokenType = "Bearer",
                    expiresIn = 86400,
                    user = UserInfo(
                        id = result.user.id,
                        email = result.user.email,
                        firstName = result.user.firstName,
                        lastName = result.user.lastName,
                        role = result.user.role.name,
                        churchId = result.user.churchId
                    )
                ))
            }
            is AuthenticationResult.EmailNotVerified -> 
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "EMAIL_NOT_VERIFIED", "message" to "Email verification required. Please check your email and verify your account."))
            is AuthenticationResult.AccountInactive -> 
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "ACCOUNT_INACTIVE", "message" to "Account is inactive. Please verify your email to activate your account."))
            is AuthenticationResult.Failure -> 
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "INVALID_CREDENTIALS", "message" to result.message))
        }
    }

    @Operation(
        summary = "User registration",
        description = "Registers a new user in the system"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User registered successfully"),
        ApiResponse(responseCode = "400", description = "Invalid registration data or password validation failed"),
        ApiResponse(responseCode = "409", description = "User with this email already exists")
    ])
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<*> {
        val command = RegisterUserCommand(
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            password = request.password,
            churchId = request.churchId ?: UUID.randomUUID()
        )
        
        return when (val result = authenticationService.register(command)) {
            is RegisterResult.Success -> ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterResponse(
                    userId = result.userId,
                    message = "User registered successfully"
                ))
            is RegisterResult.Failure -> {
                val status = if (result.message.contains("already exists")) HttpStatus.CONFLICT else HttpStatus.BAD_REQUEST
                ResponseEntity.status(status)
                    .body(mapOf("error" to "REGISTRATION_FAILED", "message" to result.message))
            }
        }
    }
}