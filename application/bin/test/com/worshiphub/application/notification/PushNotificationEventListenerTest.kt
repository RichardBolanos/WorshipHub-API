package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for PushNotificationEventListener.
 * Validates: Requirements 13.5, 29.1
 */
class PushNotificationEventListenerTest : FreeSpec({

    fun createListener(): Pair<PushNotificationEventListener, PushNotificationService> {
        val pushService = mockk<PushNotificationService>(relaxed = true)
        val listener = PushNotificationEventListener(pushService)
        return listener to pushService
    }

    "handlePushEvent" - {

        "delegates ServiceAssignment event to PushNotificationService" {
            val (listener, pushService) = createListener()
            val event = PushEvent.ServiceAssignment(
                recipientUserIds = listOf(UUID.randomUUID()),
                serviceName = "Sunday Service",
                scheduledDate = LocalDateTime.now().plusDays(1),
                roles = mapOf(UUID.randomUUID() to "Vocalist")
            )

            listener.handlePushEvent(event)

            verify { pushService.processPushEvent(event) }
        }

        "delegates ChatMessage event to PushNotificationService" {
            val (listener, pushService) = createListener()
            val event = PushEvent.ChatMessage(
                recipientUserIds = listOf(UUID.randomUUID()),
                senderName = "John",
                teamName = "Worship Team",
                messageExcerpt = "Hey team!",
                teamId = UUID.randomUUID()
            )

            listener.handlePushEvent(event)

            verify { pushService.processPushEvent(event) }
        }

        "delegates SongComment event to PushNotificationService" {
            val (listener, pushService) = createListener()
            val event = PushEvent.SongComment(
                recipientUserIds = listOf(UUID.randomUUID()),
                commenterName = "Jane",
                songTitle = "Amazing Grace",
                commentExcerpt = "Great arrangement!",
                songId = UUID.randomUUID()
            )

            listener.handlePushEvent(event)

            verify { pushService.processPushEvent(event) }
        }

        "delegates InvitationAccepted event to PushNotificationService" {
            val (listener, pushService) = createListener()
            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(UUID.randomUUID()),
                newMemberName = "Carlos",
                newMemberEmail = "carlos@test.com",
                acceptedRole = "TEAM_MEMBER"
            )

            listener.handlePushEvent(event)

            verify { pushService.processPushEvent(event) }
        }

        "delegates MemberUnavailable event to PushNotificationService" {
            val (listener, pushService) = createListener()
            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(UUID.randomUUID()),
                memberName = "Ana",
                unavailableDate = LocalDate.now().plusDays(7),
                reason = "Vacation"
            )

            listener.handlePushEvent(event)

            verify { pushService.processPushEvent(event) }
        }

        "delegates ServiceCancelled event to PushNotificationService" {
            val (listener, pushService) = createListener()
            val event = PushEvent.ServiceCancelled(
                recipientUserIds = listOf(UUID.randomUUID()),
                serviceName = "Wednesday Service",
                originalDate = LocalDateTime.now().plusDays(3),
                reason = "Weather"
            )

            listener.handlePushEvent(event)

            verify { pushService.processPushEvent(event) }
        }
    }
})
