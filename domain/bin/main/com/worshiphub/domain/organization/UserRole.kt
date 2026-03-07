package com.worshiphub.domain.organization

/**
 * User roles within the WorshipHub system.
 */
enum class UserRole {
    /**
     * Church administrator with full access to church management.
     */
    CHURCH_ADMIN,
    
    /**
     * Worship leader who can manage teams and services.
     */
    WORSHIP_LEADER,
    
    /**
     * Team member who can participate in services and view team information.
     */
    TEAM_MEMBER,
    
    /**
     * Super admin with system-wide access (for global song catalog management).
     */
    SUPER_ADMIN
}

/**
 * Extension functions for role-based authorization checks.
 */
fun UserRole.canManageChurch(): Boolean = this in listOf(UserRole.CHURCH_ADMIN, UserRole.SUPER_ADMIN)

fun UserRole.canManageTeams(): Boolean = this in listOf(UserRole.CHURCH_ADMIN, UserRole.WORSHIP_LEADER, UserRole.SUPER_ADMIN)

fun UserRole.canScheduleServices(): Boolean = this in listOf(UserRole.CHURCH_ADMIN, UserRole.WORSHIP_LEADER, UserRole.SUPER_ADMIN)

fun UserRole.canManageGlobalCatalog(): Boolean = this == UserRole.SUPER_ADMIN