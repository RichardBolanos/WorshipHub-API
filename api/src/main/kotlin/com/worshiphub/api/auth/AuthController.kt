package com.worshiphub.api.auth

import com.worshiphub.application.auth.AuthenticationService
import com.worshiphub.application.auth.AuthenticationResult
import com.worshiphub.application.auth.RegisterUserCommand
import com.worshiphub.application.auth.RegisterResult
import com.worshiphub.security.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
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
        description = "Authenticates user with email and password, returns JWT token for API access"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LoginResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Email verification required or account inactive",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    @SecurityRequirements // No security required — public endpoint
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
                    .body(ErrorResponse(error = "EMAIL_NOT_VERIFIED", message = "Email verification required. Please check your email and verify your account.", statusCode = 403))
            is AuthenticationResult.AccountInactive -> 
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse(error = "ACCOUNT_INACTIVE", message = "Account is inactive. Please verify your email to activate your account.", statusCode = 403))
            is AuthenticationResult.Failure -> 
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse(error = "INVALID_CREDENTIALS", message = result.message, statusCode = 401))
        }
    }

    @Operation(
        summary = "User registration",
        description = "Registers a new user in the system"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = RegisterResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid registration data or password validation failed",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "User with this email already exists",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    @SecurityRequirements // No security required — public endpoint
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
                    .body(ErrorResponse(error = "REGISTRATION_FAILED", message = result.message))
            }
        }
    }
}
