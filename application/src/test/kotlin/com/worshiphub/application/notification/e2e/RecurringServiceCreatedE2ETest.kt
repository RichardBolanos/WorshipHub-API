package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class RecurringServiceCreatedE2ETest : FreeSpec({

    "18.12 E2E: recurring service created sends exactly 1 consolidated notification per member" {
        val ctx = buildContext()
        val member1 = UUID.randomUUID()
        val member2 = UUID.randomUUID()

        ctx.registerToken(member1, "token-m1")
        ctx.registerToken(member2, "token-m2")
        ctx.enableAllPreferences(member1)
        ctx.enableAllPreferences(member2)

        val dates = (1..4).map { LocalDateTime.now().plusWeeks(it.toLong()) }
        val event = PushEvent.RecurringServiceCreated(
            recipientUserIds = listOf(member1, member2),
            serviceName = "Servicio Semanal",
            scheduledDates = dates,
            recurrencePattern = "WEEKLY",
            roles = mapOf(member1 to "Vocalista", member2 to "Guitarrista")
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 2
        val msg1 = ctx.gateway.findByToken("token-m1").first()
        msg1.payload.title shouldBe "Servicio recurrente creado"
        msg1.payload.body shouldContain "semanal"
        msg1.payload.body shouldContain "Vocalista"
        msg1.payload.body shouldContain "4 instancias"
        msg1.payload.channelId shouldBe "services"
        msg1.payload.data["type"] shouldBe "RECURRING_SERVICE"

        val msg2 = ctx.gateway.findByToken("token-m2").first()
        msg2.payload.body shouldContain "Guitarrista"
    }
})
