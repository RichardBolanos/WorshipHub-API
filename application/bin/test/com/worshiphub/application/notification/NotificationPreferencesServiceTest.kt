package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.NotificationPreference
import com.worshiphub.domain.collaboration.push.RoleNotificationFilter
import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.*

/**
 * Unit tests for NotificationPreferencesService (role-aware).
 * Validates: Requirements 11.1, 11.2, 11.4, 11.5, 11.6, 11.7, 29.1
 */
class NotificationPreferencesServiceTest : FreeSpec({

    fun createService(): Triple<NotificationPreferencesService, NotificationPreferenceRepository, UserRoleResolver> {
        val prefRepo = mockk<NotificationPreferenceRepository>()
        val roleResolver = mockk<UserRoleResolver>()
        val service = NotificationPreferencesService(prefRepo, roleResolver)
        return Triple(service, prefRepo, roleResolver)
    }

    "getPreferences" - {

        "returns default preferences for new user (all enabled)" {
            val (service, prefRepo, roleResolver) = createService()
            val userId = UUID.randomUUID()
            val defaultPrefs = NotificationPreference(userId = userId)

            every { prefRepo.findByUserIdOrDefault(userId) } returns defaultPrefs
            every { roleResolver.resolveEffectiveRole(userId) } returns UserRole.MEMBER

            val result = service.getPreferences(userId)

            result.isSuccess shouldBe true
            val response = result.getOrThrow()
            response.preferences.serviceAssignments shouldBe true
            response.preferences.chatMessages shouldBe true
            response.preferences.songComments shouldBe true
            response.preferences.teamChanges shouldBe true
            response.preferences.newSongs shouldBe true
        }

        "returns applicableTypes according to user role - Admin gets all" {
            val (service, prefRepo, roleResolver) = createService()
            val userId = UUID.randomUUID()
            val prefs = NotificationPreference(userId = userId)

            every { prefRepo.findByUserIdOrDefault(userId) } returns prefs
            every { roleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN

            val result = service.getPreferences(userId)

            result.isSuccess shouldBe true
            val response = result.getOrThrow()
            response.applicableTypes shouldBe NotificationType.entries.toSet()
            response.userRole shouldBe UserRole.ADMIN
        }

        "returns applicableTypes according to user role - Member gets subset" {
            val (service, prefRepo, roleResolver) = createService()
            val userId = UUID.randomUUID()
            val prefs = NotificationPreference(userId = userId)

            every { prefRepo.findByUserIdOrDefault(userId) } returns prefs
            every { roleResolver.resolveEffectiveRole(userId) } returns UserRole.MEMBER

            val result = service.getPreferences(userId)

            result.isSuccess shouldBe true
            val response = result.getOrThrow()
            response.applicableTypes shouldBe RoleNotificationFilter.getApplicableTypes(UserRole.MEMBER)
            response.userRole shouldBe UserRole.MEMBER
        }
    }

    "updatePreferences" - {

        "partial update only changes specified fields" {
            val (service, prefRepo, _) = createService()
            val userId = UUID.randomUUID()
            val existingPrefs = NotificationPreference(userId = userId)

            every { prefRepo.findByUserIdOrDefault(userId) } returns existingPrefs
            every { prefRepo.save(any()) } answers { firstArg() }

            val command = UpdatePreferencesCommand(chatMessages = false, songComments = false)
            val result = service.updatePreferences(userId, command)

            result.isSuccess shouldBe true
            val updated = result.getOrThrow()
            updated.chatMessages shouldBe false
            updated.songComments shouldBe false
            // Other fields remain true (default)
            updated.serviceAssignments shouldBe true
            updated.teamChanges shouldBe true
            updated.newSongs shouldBe true
        }
    }

    "onRoleChanged" - {

        "enables newly available types on ascending role change (Member -> TeamLeader)" {
            val (service, prefRepo, roleResolver) = createService()
            val userId = UUID.randomUUID()
            val prefs = NotificationPreference(userId = userId)

            // The user's previous role was MEMBER
            every { roleResolver.resolveEffectiveRole(userId) } returns UserRole.MEMBER
            every { prefRepo.findByUserIdOrDefault(userId) } returns prefs
            every { prefRepo.save(any()) } answers { firstArg() }

            service.onRoleChanged(userId, UserRole.TEAM_LEADER)

            // Should save with newly available types enabled
            verify {
                prefRepo.save(match { it.userId == userId })
            }
        }

        "preserves preferences for types no longer applicable on descending role change" {
            val (service, prefRepo, roleResolver) = createService()
            val userId = UUID.randomUUID()
            // User had some preferences disabled
            val prefs = NotificationPreference(userId = userId, availabilityChanges = false)

            // The user's current role is already TEAM_LEADER (same as new role or higher)
            every { roleResolver.resolveEffectiveRole(userId) } returns UserRole.TEAM_LEADER
            every { prefRepo.findByUserIdOrDefault(userId) } returns prefs

            // Changing to MEMBER — no new types become available
            service.onRoleChanged(userId, UserRole.MEMBER)

            // No save should happen since no new types are available
            verify(exactly = 0) { prefRepo.save(any()) }
        }
    }
})
