package com.worshiphub.application.auth

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class InvitationRateLimitService {
    
    private val dailyInvitationCounts = ConcurrentHashMap<String, Int>()
    private val lastResetDate = ConcurrentHashMap<String, LocalDate>()
    
    companion object {
        const val MAX_INVITATIONS_PER_DAY = 10
        const val MAX_INVITATIONS_PER_HOUR = 3
    }
    
    fun canSendInvitation(userId: UUID): Boolean {
        return try {
            val today = LocalDate.now()
            val userKey = "${userId}_${today}"
            
            // Reset counter if it's a new day
            val lastReset = lastResetDate[userId.toString()]
            if (lastReset == null || lastReset.isBefore(today)) {
                // Clean up old entries
                lastReset?.let { oldDate ->
                    dailyInvitationCounts.remove("${userId}_${oldDate}")
                }
                lastResetDate[userId.toString()] = today
            }
            
            val currentCount = dailyInvitationCounts.getOrDefault(userKey, 0)
            currentCount < MAX_INVITATIONS_PER_DAY
        } catch (e: Exception) {
            // Fail safe - deny if error occurs
            false
        }
    }
    
    fun recordInvitationSent(userId: UUID) {
        val today = LocalDate.now()
        val userKey = "${userId}_${today}"
        dailyInvitationCounts.merge(userKey, 1) { old, new -> old + new }
    }
    
    fun getRemainingInvitations(userId: UUID): Int {
        val today = LocalDate.now()
        val userKey = "${userId}_${today}"
        val currentCount = dailyInvitationCounts.getOrDefault(userKey, 0)
        return maxOf(0, MAX_INVITATIONS_PER_DAY - currentCount)
    }
    
    fun isValidEmailDomain(email: String): Boolean {
        if (email.isBlank() || !email.contains("@")) return false
        
        val emailLower = email.lowercase().trim()
        
        // Basic email format validation
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailRegex.matches(emailLower)) return false
        
        val blockedDomains = setOf(
            "10minutemail.com", "tempmail.org", "guerrillamail.com",
            "mailinator.com", "throwaway.email", "temp-mail.org",
            "yopmail.com", "sharklasers.com", "grr.la",
            "guerrillamailblock.com", "pokemail.net", "spam4.me",
            "bccto.me", "chacuo.net", "dispostable.com"
        )
        
        val domain = emailLower.substringAfter("@")
        
        // Check exact domain match
        if (blockedDomains.contains(domain)) return false
        
        // Check for suspicious patterns
        val suspiciousPatterns = listOf(
            "temp", "disposable", "fake", "trash", "spam",
            "throw", "guerrilla", "mailinator", "10min"
        )
        
        return !suspiciousPatterns.any { domain.contains(it) }
    }
}