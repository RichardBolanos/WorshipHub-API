package com.worshiphub.api.config

import org.springframework.stereotype.Component
// import com.worshiphub.infrastructure.repository.UserRepository
// import com.worshiphub.infrastructure.repository.ChurchRepository

/**
 * Simple monitoring utilities.
 */
@Component
class MonitoringService(
    // private val userRepository: UserRepository,
    // private val churchRepository: ChurchRepository
) {
    
    fun getDatabaseStatus(): Map<String, Any> {
        return try {
            // TODO: Uncomment when repositories are available
            /*
            val userCount = userRepository.count()
            val churchCount = churchRepository.count()
            
            mapOf(
                "status" to "healthy",
                "users" to userCount,
                "churches" to churchCount,
                "message" to "Database connection successful"
            )
            */
            mapOf(
                "status" to "healthy",
                "message" to "Database monitoring disabled - repositories not available"
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "unhealthy",
                "error" to "Database connection failed",
                "message" to (e.message ?: "Unknown error")
            )
        }
    }
    
    fun getJwtStatus(): Map<String, Any> {
        return try {
            val jwtSecret = System.getenv("JWT_SECRET") ?: "default"
            
            mapOf(
                "status" to "healthy",
                "secretConfigured" to (jwtSecret != "default"),
                "message" to "JWT configuration loaded"
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "unhealthy",
                "error" to "JWT configuration error"
            )
        }
    }
}