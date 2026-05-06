package com.worshiphub.security.oauth2

import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.text.ParseException
import java.util.Date

/**
 * Verifies a Google ID token (issued by Google during Sign-In flow) according to
 * Google's documented validation rules:
 *
 *   https://developers.google.com/identity/sign-in/web/backend-auth#verify-the-integrity-of-the-id-token
 *
 * Validation steps performed:
 *   1. The JWT signature is verified against Google's published JWKS keys
 *      (https://www.googleapis.com/oauth2/v3/certs). Keys are fetched on demand and
 *      cached automatically by nimbus-jose-jwt.
 *   2. The `iss` (issuer) claim must be one of:
 *        - https://accounts.google.com
 *        - accounts.google.com
 *   3. The `aud` (audience) claim must match one of the configured client IDs
 *      (`google.oauth2.allowed-client-ids`).
 *   4. The `exp` (expiration) claim must be in the future.
 *
 * Returns the verified claims on success, or null if any check fails. Callers
 * MUST treat a null result as an authentication failure.
 *
 * GraalVM native-image: this class only depends on nimbus-jose-jwt + java.net.URL
 * which are already covered by Spring Boot's OAuth2 reachability metadata.
 */
@Component
class GoogleIdTokenVerifier(
    @Value("\${google.oauth2.allowed-client-ids:}") private val allowedClientIdsCsv: String,
    @Value("\${google.oauth2.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}")
    private val jwksUri: String
) {
    private val logger = LoggerFactory.getLogger(GoogleIdTokenVerifier::class.java)

    private val allowedClientIds: Set<String> by lazy {
        allowedClientIdsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private val expectedIssuers: Set<String> = setOf(
        "https://accounts.google.com",
        "accounts.google.com"
    )

    /**
     * Lazy-initialized JWT processor. Builds a JWS key selector that resolves
     * the signing key by `kid` from Google's JWKS endpoint. The JWK source
     * has built-in caching and refresh-on-rotation behaviour.
     */
    private val jwtProcessor: DefaultJWTProcessor<SecurityContext> by lazy {
        val jwkSource = JWKSourceBuilder.create<SecurityContext>(URI(jwksUri).toURL())
            .retrying(true)
            .build()

        DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = JWSAlgorithmFamilyJWSKeySelector.fromJWKSource(jwkSource)
            // Claims verifier: expiration is checked automatically; issuer/audience
            // are checked manually below to support multiple client IDs cleanly.
            jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
                /* exactMatchClaims = */ null,
                /* requiredClaims    = */ setOf("iss", "aud", "sub", "exp", "email")
            )
        }
    }

    /**
     * Verifies a Google ID token and returns its claims if valid.
     * Returns null on any validation failure. Reasons are logged at WARN level
     * (without exposing the raw token).
     */
    fun verify(idToken: String): JWTClaimsSet? {
        if (allowedClientIds.isEmpty()) {
            logger.error(
                "Google OAuth2 is not configured. Set GOOGLE_OAUTH_CLIENT_IDS " +
                    "(comma-separated list of allowed audience values)."
            )
            return null
        }

        val jwt: SignedJWT = try {
            SignedJWT.parse(idToken)
        } catch (e: ParseException) {
            logger.warn("Rejected Google ID token: not a valid JWT")
            return null
        }

        val claims: JWTClaimsSet = try {
            jwtProcessor.process(jwt, null)
        } catch (e: Exception) {
            logger.warn("Rejected Google ID token: signature/claims verification failed ({})", e.message)
            return null
        }

        // Issuer
        val iss = claims.issuer
        if (iss !in expectedIssuers) {
            logger.warn("Rejected Google ID token: unexpected issuer '{}'", iss)
            return null
        }

        // Audience: must intersect with our allowed client IDs
        val tokenAudiences = claims.audience.orEmpty().toSet()
        if (tokenAudiences.intersect(allowedClientIds).isEmpty()) {
            logger.warn(
                "Rejected Google ID token: audience {} not in allowed client IDs",
                tokenAudiences
            )
            return null
        }

        // Expiration (defense in depth — DefaultJWTClaimsVerifier also enforces exp)
        val exp = claims.expirationTime
        if (exp == null || exp.before(Date())) {
            logger.warn("Rejected Google ID token: expired or missing exp claim")
            return null
        }

        return claims
    }
}
