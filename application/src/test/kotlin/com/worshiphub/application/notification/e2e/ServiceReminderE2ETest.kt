package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class ServiceReminderE2ETest : FreeSpec({

    "18.9 E2E: service reminder sends push to accepted members" {
        val ctx = buildContext()
        val acceptedMember = UUID.randomUUID()

        ctx.registerToken(acceptedMember, "token-accepted")
        ctx.enableAllPreferences(acceptedMember)

        val scheduledDate = LocalDateTime.now().plusHours(2)
        val event = PushEvent.ServiceReminder(
            recipientUserIds = listOf(acceptedMember),
            serviceName = "Servicio de Miércoles",
            scheduledDate = scheduledDate,
            setlistName = "Setlist Adoración",
            hoursUntil = 2
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Recordatorio de servicio"
        msg.payload.body shouldContain "Servicio de Miércoles"
        msg.payload.body shouldContain "2h"
        msg.payload.body shouldContain "Setlist Adoración"
        msg.payload.channelId shouldBe "services"
        msg.payload.data["type"] shouldBe "SERVICE_SCHEDULED"
    }
})
