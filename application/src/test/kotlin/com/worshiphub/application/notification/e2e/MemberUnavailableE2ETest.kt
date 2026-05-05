package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate
import java.util.*

class MemberUnavailableE2ETest : FreeSpec({

    "18.19 E2E: member unavailable sends push to team leaders" {
        val ctx = buildContext()
        val leaderId = UUID.randomUUID()

        ctx.registerToken(leaderId, "token-leader")
        ctx.enableAllPreferences(leaderId)

        val unavailableDate = LocalDate.now().plusDays(14)
        val event = PushEvent.MemberUnavailable(
            recipientUserIds = listOf(leaderId),
            memberName = "Juan",
            unavailableDate = unavailableDate,
            reason = "Viaje familiar"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Miembro no disponible"
        msg.payload.body shouldContain "Juan"
        msg.payload.body shouldContain "Viaje familiar"
        msg.payload.channelId shouldBe "team"
        msg.payload.data["type"] shouldBe "AVAILABILITY_CHANGE"
    }
})
