package com.worshiphub.security

import com.worshiphub.application.auth.JwtTokenService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey
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
    
    private val secretKey: SecretKey by lazy {
        try {
            if (jwtSecret.isBlank()) {
                throw IllegalStateException("JWT secret must be provided via JWT_SECRET environment variable")
            }
            Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize JWT secret key", e)
        }
    }

    override fun generateToken(userId: String, churchId: String, roles: List<String>): String {
        return try {
            require(userId.isNotBlank()) { "User ID cannot be blank" }
            require(churchId.isNotBlank()) { "Church ID cannot be blank" }
            require(roles.isNotEmpty()) { "Roles cannot be empty" }
            
            val now = Date()
            val expiryDate = Date(now.time + jwtExpiration)
            val jti = UUID.randomUUID().toString() // Unique token ID

            Jwts.builder()
                .subject(userId)
                .id(jti)
                .claim("churchId", churchId)
                .claim("roles", roles.take(10)) // Limit roles to prevent token bloat
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer("WorshipHub")
                .signWith(secretKey)
                .compact()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate JWT token", e)
        }
    }

    override fun getUserIdFromToken(token: String): String {
        require(token.isNotBlank()) { "Token cannot be blank" }
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
            claims.subject ?: throw IllegalArgumentException("Token subject is null")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid token: ${e.message}")
        }
    }

    override fun getChurchIdFromToken(token: String): String {
        require(token.isNotBlank()) { "Token cannot be blank" }
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
            claims.get("churchId", String::class.java) 
                ?: throw IllegalArgumentException("Church ID not found in token")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid token: ${e.message}")
        }
    }

    override fun getRolesFromToken(token: String): List<String> {
        require(token.isNotBlank()) { "Token cannot be blank" }
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
            
            @Suppress("UNCHECKED_CAST")
            val roles = claims.get("roles", List::class.java) as? List<String>
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
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .expiration
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
            
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer("WorshipHub")
                .build()
                .parseSignedClaims(token)
                .payload
            
            // Additional validation
            val subject = claims.subject
            val churchId = claims.get("churchId", String::class.java)
            val expiration = claims.expiration
            
            val isValid = subject != null && 
            churchId != null && 
            expiration != null && 
            expiration.after(Date())
            
            logger.debug("JWT Validation: isValid=$isValid")
            isValid
        } catch (e: Exception) {
            logger.debug("JWT Validation: Exception - ${e.message}")
            false
        }
    }
}