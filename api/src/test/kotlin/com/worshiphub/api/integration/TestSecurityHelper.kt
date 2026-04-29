package com.worshiphub.api.integration

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.util.*

/**
 * Security helper utilities for E2E integration tests.
 *
 * Configures Spring Security context so that [com.worshiphub.security.SecurityContext]
 * can read userId from `authentication.principal` (String) and churchId from
 * `authentication.details` (Map with "churchId" key).
 */
object TestSecurityHelper {

    /**
     * Sets the global [SecurityContextHolder] with the given userId, churchId and roles.
     * Useful for non-MockMvc code paths that read from the static security context.
     */
    fun mockSecurityContext(
        userId: UUID,
        churchId: UUID,
        roles: List<String> = listOf("CHURCH_ADMIN")
    ) {
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        val authentication = UsernamePasswordAuthenticationToken(
            userId.toString(),  // principal — SecurityContext.getCurrentUserId() reads this
            null,               // credentials
            authorities
        )
        authentication.details = mapOf("churchId" to churchId.toString())

        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)
    }

    /**
     * Returns a [RequestPostProcessor] that injects authentication into each MockMvc request.
     * Uses Spring Security's test support to properly integrate with the filter chain.
     */
    fun withAuth(
        userId: UUID,
        churchId: UUID,
        roles: List<String> = listOf("CHURCH_ADMIN")
    ): RequestPostProcessor {
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        val authentication = UsernamePasswordAuthenticationToken(
            userId.toString(),
            null,
            authorities
        )
        authentication.details = mapOf("churchId" to churchId.toString())

        return SecurityMockMvcRequestPostProcessors.authentication(authentication)
    }
}
