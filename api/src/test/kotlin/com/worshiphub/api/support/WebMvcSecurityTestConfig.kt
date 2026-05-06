package com.worshiphub.api.support

import com.worshiphub.security.JwtAuthenticationEntryPoint
import com.worshiphub.security.JwtAuthenticationFilter
import com.worshiphub.security.JwtTokenProvider
import com.worshiphub.security.SecurityContext
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Provides mocked security beans so `@WebMvcTest` slice tests can boot without
 * pulling the full security configuration. Pair this with
 * `@AutoConfigureMockMvc(addFilters = false)` to disable the actual filter chain
 * during the test, and `@Import(WebMvcSecurityTestConfig::class)` on the test class.
 *
 * This addresses the classic problem where `JwtAuthenticationFilter` (a
 * `@Component`) gets discovered by the slice's component scan and fails to wire
 * because `JwtTokenProvider` lives outside the slice's bean graph.
 */
@TestConfiguration
class WebMvcSecurityTestConfig {

    @Bean
    @Primary
    fun jwtTokenProvider(): JwtTokenProvider = mockk(relaxed = true)

    @Bean
    @Primary
    fun jwtAuthenticationEntryPoint(): JwtAuthenticationEntryPoint = mockk(relaxed = true)

    @Bean
    @Primary
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = mockk(relaxed = true)

    @Bean
    @Primary
    fun securityContext(): SecurityContext = mockk(relaxed = true)
}
