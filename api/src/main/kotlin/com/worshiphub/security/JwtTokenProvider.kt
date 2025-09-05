package com.worshiphub.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String
    
    @Value("\${jwt.expiration:3600000}")
    private val jwtExpiration: Long = 3600000 // 1 hour
    
    @Value("\${jwt.refresh-expiration:86400000}")
    private val refreshExpiration: Long = 86400000 // 24 hours
    
    private val blacklistedTokens = ConcurrentHashMap<String, Long>()
    
    private val secretKey: SecretKey by lazy {
        if (jwtSecret.isBlank()) {
            throw IllegalStateException("JWT secret must be provided via JWT_SECRET environment variable")
        }
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(userId: String, churchId: String, roles: List<String>): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setSubject(userId)
            .claim("churchId", churchId)
            .claim("roles", roles)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun getUserIdFromToken(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    fun getChurchIdFromToken(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .get("churchId", String::class.java)
    }

    fun getRolesFromToken(token: String): List<String> {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        
        @Suppress("UNCHECKED_CAST")
        return claims.get("roles", List::class.java) as List<String>
    }

    fun blacklistToken(token: String) {
        val expiration = getExpirationFromToken(token)
        if (expiration != null) {
            blacklistedTokens[token] = expiration.time
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
    
    fun validateToken(token: String): Boolean {
        return try {
            if (isTokenBlacklisted(token)) return false
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}