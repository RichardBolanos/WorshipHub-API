package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class RecurrenceRuleUpdatedE2ETest : FreeSpec({

    "18.13 E2E: recurrence rule updated sends push with affected dates" {
        val ctx = buildContext()
        val member = UUID.randomUUID()

        ctx.registerToken(member, "token-member")
        ctx.enableAllPreferences(member)

        val affectedDates = (1..3).map { LocalDateTime.now().plusWeeks(it.toLong()) }
        val event = PushEvent.RecurrenceRuleUpdated(
            recipientUserIds = listOf(member),
            parentServiceName = "Servicio Semanal",
            newRecurrencePattern = "BIWEEKLY",
            affectedDates = affectedDates,
            removedDates = listOf(LocalDateTime.now().plusWeeks(4))
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Recurrencia actualizada"
        msg.payload.body shouldContain "Servicio Semanal"
        msg.payload.body shouldContain "quincenal"
        msg.payload.body shouldContain "3 instancias"
        msg.payload.channelId shouldBe "services"
        msg.payload.data["type"] shouldBe "RECURRING_SERVICE"
    }
})
