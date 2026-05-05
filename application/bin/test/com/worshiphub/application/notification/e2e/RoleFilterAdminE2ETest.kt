package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.every
import java.util.*

class RoleFilterAdminE2ETest : FreeSpec({

    "18.24 E2E: Admin receives INVITATION_ACCEPTED notification" {
        val ctx = buildContext()
        val adminId = UUID.randomUUID()

        every { ctx.userRoleResolver.resolveEffectiveRole(adminId) } returns UserRole.ADMIN
        ctx.registerToken(adminId, "token-admin")
        ctx.enableAllPreferences(adminId)

        val event = PushEvent.InvitationAccepted(
            recipientUserIds = listOf(adminId),
            newMemberName = "Pedro",
            newMemberEmail = "pedro@test.com",
            acceptedRole = "MEMBER"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        ctx.gateway.findByType("INVITATION_ACCEPTED") shouldHaveSize 1
    }
})
