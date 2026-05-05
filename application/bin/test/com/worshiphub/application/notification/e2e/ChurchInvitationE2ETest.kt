package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class ChurchInvitationE2ETest : FreeSpec({

    "18.8 E2E: church invitation sends push to invited user" {
        val ctx = buildContext()
        val invitedUser = UUID.randomUUID()

        ctx.registerToken(invitedUser, "token-invited")
        ctx.enableAllPreferences(invitedUser)

        val event = PushEvent.ChurchInvitation(
            recipientUserIds = listOf(invitedUser),
            churchName = "Iglesia Vida Nueva",
            offeredRole = "Miembro"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Invitación a iglesia"
        msg.payload.body shouldContain "Iglesia Vida Nueva"
        msg.payload.body shouldContain "Miembro"
        msg.payload.channelId shouldBe "team"
    }
})
