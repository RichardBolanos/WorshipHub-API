package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class InvitationResponseE2ETest : FreeSpec({

    "18.6 E2E: invitation response sends push to team leader" {
        val ctx = buildContext()
        val leaderId = UUID.randomUUID()

        ctx.registerToken(leaderId, "token-leader")
        ctx.enableAllPreferences(leaderId)

        val scheduledDate = LocalDateTime.now().plusDays(5)
        val event = PushEvent.InvitationResponse(
            recipientUserIds = listOf(leaderId),
            memberName = "Ana",
            serviceName = "Servicio Dominical",
            scheduledDate = scheduledDate,
            accepted = true
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Respuesta a asignación"
        msg.payload.body shouldContain "Ana"
        msg.payload.body shouldContain "aceptó"
        msg.payload.body shouldContain "Servicio Dominical"
        msg.payload.channelId shouldBe "services"
        msg.payload.data["type"] shouldBe "INVITATION_RESPONSE"
    }
})
