package com.worshiphub.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsConfigurationSource: CorsConfigurationSource,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12) // Increased strength

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disabled for REST API
            .cors { it.configurationSource(corsConfigurationSource) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .contentTypeOptions { it.and() }
                    .httpStrictTransportSecurity { hstsConfig ->
                        hstsConfig
                            .maxAgeInSeconds(31536000) // 1 year
                            .includeSubDomains(true)
                    }
                    .and()
                    .headers { h -> h.cacheControl { } }
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/", "/api/v1/health", "/api/v1/system/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll() // Development only
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll() // OAuth2 endpoints
                    .requestMatchers("/api/v1/auth/email/verify/**").permitAll()
                    .requestMatchers("/api/v1/auth/password/reset/**").permitAll()
                    .requestMatchers("/api/v1/auth/password/forgot/**").permitAll()
                    .requestMatchers("/api/v1/auth/church/register").permitAll()
                    .requestMatchers("/api/v1/invitations/*/accept").permitAll()
                    .requestMatchers("/api/v1/invitations/*").permitAll() // GET invitation details
                    .requestMatchers("/actuator/**").permitAll() // Health checks
                    .requestMatchers("/api/v1/songs").permitAll() // Temporary: Allow public access to songs
                    
                    // Protected endpoints
                    .requestMatchers("/api/v1/**").authenticated()
                    .anyRequest().permitAll() // Allow other requests for development
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/oauth2/authorization/google")
                    .defaultSuccessUrl("/api/v1/auth/oauth2/google/callback", true)
                    .failureUrl("/login?error=oauth2_error")
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}