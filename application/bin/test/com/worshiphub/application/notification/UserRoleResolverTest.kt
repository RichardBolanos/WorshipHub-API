package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.*

/**
 * Unit tests for UserRoleResolver.
 * Validates: Requirements 30.5
 */
class UserRoleResolverTest : FreeSpec({

    fun createResolver(): Triple<UserRoleResolver, UserRepository, TeamRepository> {
        val userRepository = mockk<UserRepository>()
        val teamRepository = mockk<TeamRepository>()
        val resolver = UserRoleResolver(userRepository, teamRepository)
        return Triple(resolver, userRepository, teamRepository)
    }

    val churchId = UUID.randomUUID()

    fun createUser(
        id: UUID = UUID.randomUUID(),
        role: com.worshiphub.domain.organization.UserRole = com.worshiphub.domain.organization.UserRole.TEAM_MEMBER
    ) = User(
        id = id,
        email = "user@test.com",
        firstName = "Test",
        lastName = "User",
        passwordHash = "hash",
        churchId = churchId,
        role = role
    )

    "resolveEffectiveRole" - {

        "returns ADMIN when user has CHURCH_ADMIN role" {
            val (resolver, userRepo, _) = createResolver()
            val userId = UUID.randomUUID()
            val user = createUser(id = userId, role = com.worshiphub.domain.organization.UserRole.CHURCH_ADMIN)

            every { userRepo.findById(userId) } returns user

            resolver.resolveEffectiveRole(userId) shouldBe UserRole.ADMIN
        }

        "returns ADMIN when user has SUPER_ADMIN role" {
            val (resolver, userRepo, _) = createResolver()
            val userId = UUID.randomUUID()
            val user = createUser(id = userId, role = com.worshiphub.domain.organization.UserRole.SUPER_ADMIN)

            every { userRepo.findById(userId) } returns user

            resolver.resolveEffectiveRole(userId) shouldBe UserRole.ADMIN
        }

        "returns TEAM_LEADER when user has WORSHIP_LEADER role" {
            val (resolver, userRepo, _) = createResolver()
            val userId = UUID.randomUUID()
            val user = createUser(id = userId, role = com.worshiphub.domain.organization.UserRole.WORSHIP_LEADER)

            every { userRepo.findById(userId) } returns user

            resolver.resolveEffectiveRole(userId) shouldBe UserRole.TEAM_LEADER
        }

        "returns TEAM_LEADER when user is leader of at least one team" {
            val (resolver, userRepo, teamRepo) = createResolver()
            val userId = UUID.randomUUID()
            val user = createUser(id = userId, role = com.worshiphub.domain.organization.UserRole.TEAM_MEMBER)
            val team = Team(
                name = "Worship Team",
                churchId = churchId,
                leaderId = userId
            )

            every { userRepo.findById(userId) } returns user
            every { teamRepo.findByLeaderId(userId) } returns listOf(team)

            resolver.resolveEffectiveRole(userId) shouldBe UserRole.TEAM_LEADER
        }

        "returns MEMBER when user is a regular team member with no leadership" {
            val (resolver, userRepo, teamRepo) = createResolver()
            val userId = UUID.randomUUID()
            val user = createUser(id = userId, role = com.worshiphub.domain.organization.UserRole.TEAM_MEMBER)

            every { userRepo.findById(userId) } returns user
            every { teamRepo.findByLeaderId(userId) } returns emptyList()

            resolver.resolveEffectiveRole(userId) shouldBe UserRole.MEMBER
        }

        "returns MEMBER when user is not found" {
            val (resolver, userRepo, _) = createResolver()
            val userId = UUID.randomUUID()

            every { userRepo.findById(userId) } returns null

            resolver.resolveEffectiveRole(userId) shouldBe UserRole.MEMBER
        }
    }
})
