package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class RecurringServiceDeletedE2ETest : FreeSpec({

    "18.14 E2E: recurring service deleted sends push to members of deleted instances" {
        val ctx = buildContext()
        val member = UUID.randomUUID()

        ctx.registerToken(member, "token-member")
        ctx.enableAllPreferences(member)

        val affectedDates = (1..5).map { LocalDateTime.now().plusWeeks(it.toLong()) }
        val event = PushEvent.RecurringServiceDeleted(
            recipientUserIds = listOf(member),
            serviceName = "Servicio Semanal",
            affectedDates = affectedDates,
            reason = "Reestructuración de horarios"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Servicio recurrente eliminado"
        msg.payload.body shouldContain "Servicio Semanal"
        msg.payload.body shouldContain "5 instancias"
        msg.payload.body shouldContain "Reestructuración de horarios"
        msg.payload.channelId shouldBe "services"
    }
})
