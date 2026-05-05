package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.every
import io.mockk.verify
import java.util.*

class RoleFilterLeaderE2ETest : FreeSpec({

    "18.26 E2E: TeamLeader does NOT receive INVITATION_ACCEPTED notification (Admin-only)" {
        val ctx = buildContext()
        val leaderId = UUID.randomUUID()

        every { ctx.userRoleResolver.resolveEffectiveRole(leaderId) } returns UserRole.TEAM_LEADER
        ctx.registerToken(leaderId, "token-leader")
        ctx.enableAllPreferences(leaderId)

        val event = PushEvent.InvitationAccepted(
            recipientUserIds = listOf(leaderId),
            newMemberName = "Pedro",
            newMemberEmail = "pedro@test.com",
            acceptedRole = "MEMBER"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages.shouldBeEmpty()
        verify(exactly = 0) {
            ctx.notificationAppService.sendNotification(
                userId = leaderId,
                title = any(),
                message = any(),
                type = any(),
                relatedEntityId = any(),
                relatedEntityType = any()
            )
        }
    }
})
