package com.worshiphub.api.config

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Audit logging for sensitive operations.
 */
@Aspect
@Component
class AuditLoggingAspect {

    @Before("@annotation(org.springframework.web.bind.annotation.PostMapping) && " +
            "execution(* com.worshiphub.api.auth.AuthController.*(..))")
    fun logAuthenticationAttempt(joinPoint: JoinPoint) {
        val methodName = joinPoint.signature.name
        val timestamp = LocalDateTime.now()
        
        println("AUDIT: Authentication attempt - Method: $methodName, Time: $timestamp")
    }

    @AfterReturning("@annotation(org.springframework.web.bind.annotation.PostMapping) && " +
                   "execution(* com.worshiphub.api.organization.ChurchController.*(..))")
    fun logChurchOperation(joinPoint: JoinPoint) {
        val user = getCurrentUser()
        val methodName = joinPoint.signature.name
        val timestamp = LocalDateTime.now()
        
        println("AUDIT: Church operation - User: $user, Method: $methodName, Time: $timestamp")
    }

    @AfterReturning("@annotation(org.springframework.web.bind.annotation.PostMapping) && " +
                   "execution(* com.worshiphub.api.organization.TeamController.*(..))")
    fun logTeamOperation(joinPoint: JoinPoint) {
        val user = getCurrentUser()
        val methodName = joinPoint.signature.name
        val timestamp = LocalDateTime.now()
        
        println("AUDIT: Team operation - User: $user, Method: $methodName, Time: $timestamp")
    }

    @AfterReturning("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    fun logDeleteOperation(joinPoint: JoinPoint) {
        val user = getCurrentUser()
        val methodName = joinPoint.signature.name
        val className = joinPoint.signature.declaringTypeName
        val timestamp = LocalDateTime.now()
        
        println("AUDIT: DELETE operation - User: $user, Class: $className, Method: $methodName, Time: $timestamp")
    }

    private fun getCurrentUser(): String {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            authentication?.name ?: "anonymous"
        } catch (e: Exception) {
            "unknown"
        }
    }
}