package com.worshiphub.application.notification.e2e

import com.worshiphub.application.notification.NotificationPreferencesService
import com.worshiphub.application.notification.UserRoleResolver
import com.worshiphub.domain.collaboration.push.NotificationPreference
import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.*

class RoleChangeDescendingE2ETest : FreeSpec({

    "18.28 E2E: descending role change hides non-applicable types but preserves in DB" {
        val prefRepo = mockk<NotificationPreferenceRepository>()
        val userRoleResolver = mockk<UserRoleResolver>()
        val userId = UUID.randomUUID()

        every { userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.MEMBER

        val existingPrefs = NotificationPreference(
            userId = userId,
            availabilityChanges = true,
            invitationAccepted = true
        )
        every { prefRepo.findByUserIdOrDefault(userId) } returns existingPrefs
        every { prefRepo.save(any()) } answers { firstArg() }

        val prefsService = NotificationPreferencesService(prefRepo, userRoleResolver)

        val result = prefsService.getPreferences(userId)
        result.isSuccess shouldBe true
        val response = result.getOrNull()!!

        response.userRole shouldBe UserRole.MEMBER
        response.applicableTypes.contains(NotificationType.AVAILABILITY_CHANGE) shouldBe false
        response.applicableTypes.contains(NotificationType.INVITATION_ACCEPTED) shouldBe false

        // Underlying preferences still have them enabled (preserved in DB)
        response.preferences.availabilityChanges shouldBe true
        response.preferences.invitationAccepted shouldBe true

        // Visible preferences should only contain MEMBER types
        val visible = response.getVisiblePreferences()
        visible.containsKey(NotificationType.AVAILABILITY_CHANGE) shouldBe false
        visible.containsKey(NotificationType.SERVICE_INVITATION) shouldBe true
    }
})
