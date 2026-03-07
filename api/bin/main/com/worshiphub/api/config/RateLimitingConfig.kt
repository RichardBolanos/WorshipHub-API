package com.worshiphub.api.config

import org.springframework.stereotype.Component
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Simple rate limiting implementation.
 */
@Component
class RateLimitingFilter : Filter {
    
    private val requestCounts = ConcurrentHashMap<String, RequestCounter>()
    private val maxRequestsPerMinute = 100
    
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        
        val clientIp = getClientIp(httpRequest)
        
        if (isRateLimited(clientIp)) {
            httpResponse.status = 429
            httpResponse.setHeader("Retry-After", "60")
            httpResponse.writer.write("{\"error\":\"Rate limit exceeded\"}")
            return
        }
        
        chain.doFilter(request, response)
    }
    
    private fun getClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")
            ?: request.getHeader("X-Real-IP")
            ?: request.remoteAddr
    }
    
    private fun isRateLimited(clientIp: String): Boolean {
        val now = LocalDateTime.now()
        val counter = requestCounts.computeIfAbsent(clientIp) { RequestCounter() }
        
        // Reset counter if more than 1 minute has passed
        if (ChronoUnit.MINUTES.between(counter.windowStart, now) >= 1) {
            counter.count.set(0)
            counter.windowStart = now
        }
        
        return counter.count.incrementAndGet() > maxRequestsPerMinute
    }
    
    private data class RequestCounter(
        val count: AtomicInteger = AtomicInteger(0),
        var windowStart: LocalDateTime = LocalDateTime.now()
    )
}