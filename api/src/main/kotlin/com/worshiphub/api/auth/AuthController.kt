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
        ApiResponse(responseCode = "400", description = "Invalid request data")
    ])
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        return when (val result = authenticationService.authenticate(request.email, request.password)) {
            is AuthenticationResult.Success -> {
                val token = jwtTokenProvider.generateToken(
                    userId = result.user.id.toString(),
                    churchId = result.user.churchId.toString(),
                    roles = listOf(result.user.role.name)
                )
                LoginResponse(
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
                )
            }
            is AuthenticationResult.EmailNotVerified -> throw RuntimeException("Email verification required. Please check your email and verify your account.")
            is AuthenticationResult.Failure -> throw RuntimeException(result.message)
        }
    }

    @Operation(
        summary = "User registration",
        description = "Registers a new user in the system"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User registered successfully"),
        ApiResponse(responseCode = "400", description = "Invalid registration data"),
        ApiResponse(responseCode = "409", description = "User already exists")
    ])
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): RegisterResponse {
        val command = RegisterUserCommand(
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            password = request.password,
            churchId = request.churchId ?: UUID.randomUUID()
        )
        
        return when (val result = authenticationService.register(command)) {
            is RegisterResult.Success -> RegisterResponse(
                userId = result.userId,
                message = "User registered successfully"
            )
            is RegisterResult.Failure -> throw RuntimeException(result.message)
        }
    }
}