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

class PreferencesApiResponseE2ETest : FreeSpec({

    "18.29 E2E: preferences response includes applicableTypes for MEMBER role" {
        val prefRepo = mockk<NotificationPreferenceRepository>()
        val userRoleResolver = mockk<UserRoleResolver>()
        val userId = UUID.randomUUID()

        every { userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.MEMBER
        every { prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(userId = userId)

        val prefsService = NotificationPreferencesService(prefRepo, userRoleResolver)
        val result = prefsService.getPreferences(userId)

        result.isSuccess shouldBe true
        val response = result.getOrNull()!!

        response.userRole shouldBe UserRole.MEMBER

        val expectedTypes = RoleNotificationFilter.getApplicableTypes(UserRole.MEMBER)
        response.applicableTypes shouldBe expectedTypes

        // MEMBER should have these types
        response.applicableTypes.contains(NotificationType.SERVICE_INVITATION) shouldBe true
        response.applicableTypes.contains(NotificationType.CHAT_MESSAGE) shouldBe true
        response.applicableTypes.contains(NotificationType.NEW_COMMENT) shouldBe true
        response.applicableTypes.contains(NotificationType.NEW_SONG) shouldBe true
        response.applicableTypes.contains(NotificationType.SERVICE_SCHEDULED) shouldBe true
        response.applicableTypes.contains(NotificationType.SERVICE_CANCELLED) shouldBe true
        response.applicableTypes.contains(NotificationType.RECURRING_SERVICE) shouldBe true
        response.applicableTypes.contains(NotificationType.SONG_UPDATED) shouldBe true
        response.applicableTypes.contains(NotificationType.SONG_DELETED) shouldBe true
        response.applicableTypes.contains(NotificationType.SONG_ATTACHMENT) shouldBe true

        // MEMBER should NOT have these types
        response.applicableTypes.contains(NotificationType.INVITATION_ACCEPTED) shouldBe false
        response.applicableTypes.contains(NotificationType.AVAILABILITY_CHANGE) shouldBe false
        response.applicableTypes.contains(NotificationType.TEAM_MEMBER_ADDED) shouldBe false
        response.applicableTypes.contains(NotificationType.TEAM_MEMBER_REMOVED) shouldBe false
        response.applicableTypes.contains(NotificationType.TEAM_LEADER_CHANGED) shouldBe false
        response.applicableTypes.contains(NotificationType.TEAM_ROLE_CHANGED) shouldBe false
    }
})
