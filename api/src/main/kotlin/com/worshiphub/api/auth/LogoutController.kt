package com.worshiphub.api.auth

import com.worshiphub.security.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
class LogoutController(
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Operation(
        summary = "User logout",
        description = "Blacklists the current JWT token to prevent further use",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val authHeader = request.getHeader("Authorization")
        if (authHeader?.startsWith("Bearer ") == true) {
            val token = authHeader.substring(7)
            jwtTokenProvider.blacklistToken(token)
        }
        
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}