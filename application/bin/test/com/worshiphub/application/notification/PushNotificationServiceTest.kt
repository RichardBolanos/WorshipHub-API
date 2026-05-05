package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.*
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.mockk.*
import java.util.*

/**
 * Unit tests for role-based filtering in PushNotificationService.
 * Validates: Requirements 30.1, 30.2, 30.4, 29.1
 */
class PushNotificationServiceTest : FreeSpec({

    fun createService(): PushNotificationServiceTestContext {
        val pushGateway = mockk<PushGateway>(relaxed = true)
        val deviceTokenRepo = mockk<DeviceTokenRepository>()
        val prefRepo = mockk<NotificationPreferenceRepository>()
        val notificationAppService = mockk<NotificationApplicationService>(relaxed = true)
        val userRoleResolver = mockk<UserRoleResolver>()
        val service = PushNotificationService(
            pushGateway, deviceTokenRepo, prefRepo, notificationAppService, userRoleResolver
        )
        return PushNotificationServiceTestContext(
            service, pushGateway, deviceTokenRepo, prefRepo, notificationAppService, userRoleResolver
        )
    }

    "role-based filtering" - {

        "skips push for Member when type is INVITATION_ACCEPTED" {
            val ctx = createService()
            val memberId = UUID.randomUUID()
            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(memberId),
                newMemberName = "Carlos",
                newMemberEmail = "carlos@test.com",
                acceptedRole = "TEAM_MEMBER"
            )

            every { ctx.userRoleResolver.resolveEffectiveRole(memberId) } returns UserRole.MEMBER

            ctx.service.processPushEvent(event)

            // Member is filtered out — no in-app notification, no push
            verify(exactly = 0) {
                ctx.notificationAppService.sendNotification(
                    userId = memberId,
                    title = any(),
                    message = any(),
                    type = any(),
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "sends push to Admin for any notification type" {
            val ctx = createService()
            val adminId = UUID.randomUUID()
            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(adminId),
                newMemberName = "Carlos",
                newMemberEmail = "carlos@test.com",
                acceptedRole = "TEAM_MEMBER"
            )
            val token = DeviceToken(
                userId = adminId,
                token = "admin-fcm-token-12345678",
                platform = DevicePlatform.ANDROID
            )

            every { ctx.userRoleResolver.resolveEffectiveRole(adminId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(adminId) } returns NotificationPreference(userId = adminId)
            every { ctx.deviceTokenRepo.findByUserId(adminId) } returns listOf(token)
            every { ctx.pushGateway.sendToDevices(any(), any()) } returns listOf(PushResult.Success("msg-1"))

            ctx.service.processPushEvent(event)

            verify {
                ctx.notificationAppService.sendNotification(
                    userId = adminId,
                    title = any(),
                    message = any(),
                    type = NotificationType.INVITATION_ACCEPTED,
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
            verify { ctx.pushGateway.sendToDevices(listOf("admin-fcm-token-12345678"), any()) }
        }

        "skips push for Member when type is AVAILABILITY_CHANGE" {
            val ctx = createService()
            val memberId = UUID.randomUUID()
            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(memberId),
                memberName = "Ana",
                unavailableDate = java.time.LocalDate.now().plusDays(7),
                reason = "Vacation"
            )

            every { ctx.userRoleResolver.resolveEffectiveRole(memberId) } returns UserRole.MEMBER

            ctx.service.processPushEvent(event)

            verify(exactly = 0) {
                ctx.notificationAppService.sendNotification(
                    userId = memberId,
                    title = any(),
                    message = any(),
                    type = any(),
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
        }

        "sends push to TeamLeader when type is AVAILABILITY_CHANGE" {
            val ctx = createService()
            val leaderId = UUID.randomUUID()
            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(leaderId),
                memberName = "Ana",
                unavailableDate = java.time.LocalDate.now().plusDays(7),
                reason = "Vacation"
            )
            val token = DeviceToken(
                userId = leaderId,
                token = "leader-fcm-token-12345678",
                platform = DevicePlatform.ANDROID
            )

            every { ctx.userRoleResolver.resolveEffectiveRole(leaderId) } returns UserRole.TEAM_LEADER
            every { ctx.prefRepo.findByUserIdOrDefault(leaderId) } returns NotificationPreference(userId = leaderId)
            every { ctx.deviceTokenRepo.findByUserId(leaderId) } returns listOf(token)
            every { ctx.pushGateway.sendToDevices(any(), any()) } returns listOf(PushResult.Success("msg-2"))

            ctx.service.processPushEvent(event)

            verify {
                ctx.notificationAppService.sendNotification(
                    userId = leaderId,
                    title = any(),
                    message = any(),
                    type = NotificationType.AVAILABILITY_CHANGE,
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
        }

        "in-app notification is NOT stored for users filtered by role" {
            val ctx = createService()
            val memberId = UUID.randomUUID()
            val adminId = UUID.randomUUID()
            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(memberId, adminId),
                newMemberName = "Carlos",
                newMemberEmail = "carlos@test.com",
                acceptedRole = "TEAM_MEMBER"
            )
            val token = DeviceToken(
                userId = adminId,
                token = "admin-fcm-token-12345678",
                platform = DevicePlatform.ANDROID
            )

            every { ctx.userRoleResolver.resolveEffectiveRole(memberId) } returns UserRole.MEMBER
            every { ctx.userRoleResolver.resolveEffectiveRole(adminId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(adminId) } returns NotificationPreference(userId = adminId)
            every { ctx.deviceTokenRepo.findByUserId(adminId) } returns listOf(token)
            every { ctx.pushGateway.sendToDevices(any(), any()) } returns listOf(PushResult.Success("msg-3"))

            ctx.service.processPushEvent(event)

            // Member should NOT get in-app notification (filtered by role)
            verify(exactly = 0) {
                ctx.notificationAppService.sendNotification(
                    userId = memberId,
                    title = any(),
                    message = any(),
                    type = any(),
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
            // Admin SHOULD get in-app notification
            verify(exactly = 1) {
                ctx.notificationAppService.sendNotification(
                    userId = adminId,
                    title = any(),
                    message = any(),
                    type = NotificationType.INVITATION_ACCEPTED,
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
        }
    }
})

/**
 * Helper context for PushNotificationService tests.
 */
data class PushNotificationServiceTestContext(
    val service: PushNotificationService,
    val pushGateway: PushGateway,
    val deviceTokenRepo: DeviceTokenRepository,
    val prefRepo: NotificationPreferenceRepository,
    val notificationAppService: NotificationApplicationService,
    val userRoleResolver: UserRoleResolver
)
