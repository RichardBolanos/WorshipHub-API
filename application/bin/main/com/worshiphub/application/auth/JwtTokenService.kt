package com.worshiphub.application.auth

/**
 * Interface for JWT token operations.
 * Implementation will be provided in infrastructure layer.
 */
interface JwtTokenService {
    
    /**
     * Generates a JWT token for the given user.
     */
    fun generateToken(userId: String, churchId: String, roles: List<String>): String
    
    /**
     * Validates a JWT token.
     */
    fun validateToken(token: String): Boolean
    
    /**
     * Extracts user ID from token.
     */
    fun getUserIdFromToken(token: String): String
    
    /**
     * Extracts church ID from token.
     */
    fun getChurchIdFromToken(token: String): String
    
    /**
     * Extracts roles from token.
     */
    fun getRolesFromToken(token: String): List<String>
    
    /**
     * Blacklists a token.
     */
    fun blacklistToken(token: String)
}