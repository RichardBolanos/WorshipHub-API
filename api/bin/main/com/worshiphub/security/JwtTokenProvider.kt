package com.worshiphub.security

import com.worshiphub.application.auth.JwtTokenService
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

@Component
class JwtTokenProvider : JwtTokenService {

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration:3600000}")
    private val jwtExpiration: Long = 3600000 // 1 hour

    @Value("\${jwt.refresh-expiration:86400000}")
    private val refreshExpiration: Long = 86400000 // 24 hours

    private val blacklistedTokens = ConcurrentHashMap<String, Long>()

    private val signer: MACSigner by lazy {
        if (jwtSecret.isBlank()) {
            throw IllegalStateException("JWT secret must be provided via JWT_SECRET environment variable")
        }
        // Pad or hash the secret to ensure it's at least 256 bits (32 bytes) for HS256
        val keyBytes = jwtSecret.toByteArray().let { bytes ->
            if (bytes.size >= 32) bytes
            else bytes.copyOf(32) // zero-pad to 32 bytes
        }
        MACSigner(keyBytes)
    }

    private val verifier: MACVerifier by lazy {
        val keyBytes = jwtSecret.toByteArray().let { bytes ->
            if (bytes.size >= 32) bytes
            else bytes.copyOf(32)
        }
        MACVerifier(keyBytes)
    }

    override fun generateToken(userId: String, churchId: String, roles: List<String>): String {
        return try {
            require(userId.isNotBlank()) { "User ID cannot be blank" }
            require(churchId.isNotBlank()) { "Church ID cannot be blank" }
            require(roles.isNotEmpty()) { "Roles cannot be empty" }

            val now = Date()
            val expiryDate = Date(now.time + jwtExpiration)
            val jti = UUID.randomUUID().toString()

            val claimsSet = JWTClaimsSet.Builder()
                .subject(userId)
                .jwtID(jti)
                .claim("churchId", churchId)
                .claim("roles", roles.take(10))
                .issueTime(now)
                .expirationTime(expiryDate)
                .issuer("WorshipHub")
                .build()

            val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet)
            signedJWT.sign(signer)
            signedJWT.serialize()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate JWT token", e)
        }
    }

    override fun getUserIdFromToken(token: String): String {
        require(token.isNotBlank()) { "Token cannot be blank" }
        return try {
            val claims = parseAndVerify(token)
            claims.subject ?: throw IllegalArgumentException("Token subject is null")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid token: ${e.message}")
        }
    }

    override fun getChurchIdFromToken(token: String): String {
        require(token.isNotBlank()) { "Token cannot be blank" }
        return try {
            val claims = parseAndVerify(token)
            claims.getStringClaim("churchId")
                ?: throw IllegalArgumentException("Church ID not found in token")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid token: ${e.message}")
        }
    }

    override fun getRolesFromToken(token: String): List<String> {
        require(token.isNotBlank()) { "Token cannot be blank" }
        return try {
            val claims = parseAndVerify(token)
            @Suppress("UNCHECKED_CAST")
            val roles = claims.getStringListClaim("roles")
            roles ?: emptyList()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid token: ${e.message}")
        }
    }

    override fun blacklistToken(token: String) {
        try {
            require(token.isNotBlank()) { "Token cannot be blank" }
            val expiration = getExpirationFromToken(token)
            if (expiration != null) {
                blacklistedTokens[token] = expiration.time
            }
        } catch (e: Exception) {
            // Log error but don't throw - blacklisting should be best effort
        }
    }

    fun isTokenBlacklisted(token: String): Boolean {
        cleanupExpiredTokens()
        return blacklistedTokens.containsKey(token)
    }

    private fun cleanupExpiredTokens() {
        val now = System.currentTimeMillis()
        blacklistedTokens.entries.removeIf { it.value < now }
    }

    private fun getExpirationFromToken(token: String): Date? {
        return try {
            val claims = parseAndVerify(token)
            claims.expirationTime
        } catch (e: Exception) {
            null
        }
    }

    override fun validateToken(token: String): Boolean {
        if (token.isBlank()) {
            logger.debug("JWT Validation: Token is blank")
            return false
        }

        return try {
            if (isTokenBlacklisted(token)) {
                logger.debug("JWT Validation: Token is blacklisted")
                return false
            }

            val claims = parseAndVerify(token)

            val subject = claims.subject
            val churchId = claims.getStringClaim("churchId")
            val expiration = claims.expirationTime
            val issuer = claims.issuer

            val isValid = subject != null &&
                churchId != null &&
                expiration != null &&
                expiration.after(Date()) &&
                issuer == "WorshipHub"

            logger.debug("JWT Validation: isValid=$isValid")
            isValid
        } catch (e: Exception) {
            logger.debug("JWT Validation: Exception - ${e.message}")
            false
        }
    }

    /**
     * Parses and verifies a JWT token, returning the claims set.
     * Throws if the signature is invalid or the token is malformed.
     */
    private fun parseAndVerify(token: String): JWTClaimsSet {
        val signedJWT = SignedJWT.parse(token)
        if (!signedJWT.verify(verifier)) {
            throw IllegalArgumentException("JWT signature verification failed")
        }
        return signedJWT.jwtClaimsSet
    }
}
