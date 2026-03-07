package com.worshiphub.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {
    
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val isTokenExpired = request.getAttribute("jwt_expired") == true
        
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        
        val message = if (isTokenExpired) "Token Expired" else "Unauthorized"
        response.writer.write("""{"error": "$message", "status": 401}""")
    }
}