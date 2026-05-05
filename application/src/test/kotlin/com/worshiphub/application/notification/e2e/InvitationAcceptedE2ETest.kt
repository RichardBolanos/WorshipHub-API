package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class InvitationAcceptedE2ETest : FreeSpec({

    "18.18 E2E: invitation accepted sends push to admin who invited" {
        val ctx = buildContext()
        val adminId = UUID.randomUUID()

        ctx.registerToken(adminId, "token-admin")
        ctx.enableAllPreferences(adminId)

        val event = PushEvent.InvitationAccepted(
            recipientUserIds = listOf(adminId),
            newMemberName = "María López",
            newMemberEmail = "maria@example.com",
            acceptedRole = "TEAM_MEMBER"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Invitación aceptada"
        msg.payload.body shouldContain "María López"
        msg.payload.body shouldContain "maria@example.com"
        msg.payload.body shouldContain "TEAM_MEMBER"
        msg.payload.channelId shouldBe "team"
        msg.payload.data["type"] shouldBe "INVITATION_ACCEPTED"
    }
})
