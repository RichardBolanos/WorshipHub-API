package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.util.*

class ServiceCancelledE2ETest : FreeSpec({

    "18.11 E2E: service cancelled sends push to assigned members" {
        val ctx = buildContext()
        val member = UUID.randomUUID()

        ctx.registerToken(member, "token-member")
        ctx.enableAllPreferences(member)

        val originalDate = LocalDateTime.now().plusDays(4)
        val event = PushEvent.ServiceCancelled(
            recipientUserIds = listOf(member),
            serviceName = "Servicio de Viernes",
            originalDate = originalDate,
            reason = "Mantenimiento del templo"
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Servicio cancelado"
        msg.payload.body shouldContain "Servicio de Viernes"
        msg.payload.body shouldContain "Mantenimiento del templo"
        msg.payload.channelId shouldBe "services"
        msg.payload.data["type"] shouldBe "SERVICE_CANCELLED"
    }
})
