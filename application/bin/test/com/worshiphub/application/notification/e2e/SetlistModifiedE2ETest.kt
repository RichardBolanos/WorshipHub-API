package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class SetlistModifiedE2ETest : FreeSpec({

    "18.10 E2E: setlist modified sends push to assigned members" {
        val ctx = buildContext()
        val member = UUID.randomUUID()
        val serviceId = UUID.randomUUID()

        ctx.registerToken(member, "token-member")
        ctx.enableAllPreferences(member)

        val scheduledDate = LocalDateTime.now().plusDays(2)
        val event = PushEvent.SetlistModified(
            recipientUserIds = listOf(member),
            serviceName = "Servicio Dominical",
            scheduledDate = scheduledDate,
            changeSummary = "Se agregó 'Oceans' y se removió 'Way Maker'",
            serviceId = serviceId
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Setlist modificado"
        msg.payload.body shouldContain "Servicio Dominical"
        msg.payload.body shouldContain "Oceans"
        msg.payload.channelId shouldBe "services"
        msg.payload.data["type"] shouldBe "SETLIST_MODIFIED"
        msg.payload.data["entityId"] shouldBe serviceId.toString()
    }
})
