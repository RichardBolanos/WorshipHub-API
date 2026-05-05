package com.worshiphub.application.notification.e2e

import com.worshiphub.application.notification.NotificationPreferencesService
import com.worshiphub.application.notification.UserRoleResolver
import com.worshiphub.domain.collaboration.push.NotificationPreference
import com.worshiphub.domain.collaboration.push.RoleNotificationFilter
import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.*

class RoleChangeAscendingE2ETest : FreeSpec({

    "18.27 E2E: ascending role change enables newly available notification types" {
        val prefRepo = mockk<NotificationPreferenceRepository>()
        val userRoleResolver = mockk<UserRoleResolver>()
        val userId = UUID.randomUUID()

        every { userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.TEAM_LEADER

        val existingPrefs = NotificationPreference(userId = userId)
        every { prefRepo.findByUserIdOrDefault(userId) } returns existingPrefs
        every { prefRepo.save(any()) } answers { firstArg() }

        val prefsService = NotificationPreferencesService(prefRepo, userRoleResolver)

        prefsService.onRoleChanged(userId, UserRole.TEAM_LEADER)

        val prefsResult = prefsService.getPreferences(userId)
        prefsResult.isSuccess shouldBe true
        val response = prefsResult.getOrNull()!!
        response.userRole shouldBe UserRole.TEAM_LEADER
        response.applicableTypes shouldBe RoleNotificationFilter.getApplicableTypes(UserRole.TEAM_LEADER)

        response.applicableTypes.contains(NotificationType.AVAILABILITY_CHANGE) shouldBe true
        response.applicableTypes.contains(NotificationType.INVITATION_ACCEPTED) shouldBe false
    }
})
