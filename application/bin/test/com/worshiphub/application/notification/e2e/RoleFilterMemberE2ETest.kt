package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.every
import io.mockk.verify
import java.util.*

class RoleFilterMemberE2ETest : FreeSpec({

    "18.25 E2E: Member does NOT receive INVITATION_ACCEPTED notification" {
        val ctx = buildContext()
        val memberId = UUID.randomUUID()

        every { ctx.userRoleResolver.resolveEffectiveRole(memberId) } returns UserRole.MEMBER
        ctx.registerToken(memberId, "token-member")
        ctx.enableAllPreferences(memberId)

        val event = PushEvent.InvitationAccepted(
            recipientUserIds = listOf(memberId),
            newMemberName = "Pedro",
            newMemberEmail = "pedro@test.com",
            acceptedRole = "MEMBER"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages.shouldBeEmpty()
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
})
