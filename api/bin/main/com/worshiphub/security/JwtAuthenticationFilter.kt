package com.worshiphub.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)
            logger.debug("JWT Filter - Token extracted: ${if (token != null) "[PRESENT]" else "[MISSING]"} for ${request.requestURI}")
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                logger.debug("JWT Filter - Token validated successfully")
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                val churchId = jwtTokenProvider.getChurchIdFromToken(token)
                val roles = jwtTokenProvider.getRolesFromToken(token)
                
                logger.debug("JWT Filter - Extracted: userId=$userId, churchId=$churchId, roles=$roles")
                
                if (userId.isBlank() || churchId.isBlank()) {
                    logger.debug("JWT Filter - Invalid token data: userId or churchId is blank")
                    return@doFilterInternal
                }
            
            // Create authentication with user roles (add ROLE_ prefix for Spring Security)
            val authorities = roles.map { role ->
                if (role.startsWith("ROLE_")) {
                    SimpleGrantedAuthority(role)
                } else {
                    SimpleGrantedAuthority("ROLE_$role")
                }
            }
            val authentication = UsernamePasswordAuthenticationToken(
                userId, null, authorities
            )
            
            // Add church context to authentication details
            authentication.details = mapOf(
                "userId" to userId,
                "churchId" to churchId
            )
            
                SecurityContextHolder.getContext().authentication = authentication
                logger.debug("JWT Filter - Authentication set for user: $userId")
            } else {
                logger.debug("JWT Filter - Token validation failed or token is null")
            }
        } catch (e: Exception) {
            logger.debug("JWT Filter - Exception occurred: ${e.message}")
            SecurityContextHolder.clearContext()
            
            // Set token expiration info in request for error handling
            if (e.message?.contains("expired") == true) {
                request.setAttribute("jwt_expired", true)
            }
        }
        
        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}