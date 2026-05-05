package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.verify
import java.time.LocalDateTime
import java.util.*

class ServiceAssignmentE2ETest : FreeSpec({

    "18.2 E2E: service assignment sends push with correct payload" {
        val ctx = buildContext()
        val memberId = UUID.randomUUID()
        val token = ctx.registerToken(memberId)
        ctx.enableAllPreferences(memberId)

        val scheduledDate = LocalDateTime.now().plusDays(3)
        val event = PushEvent.ServiceAssignment(
            recipientUserIds = listOf(memberId),
            serviceName = "Domingo de Adoración",
            scheduledDate = scheduledDate,
            roles = mapOf(memberId to "Vocalista")
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.token shouldBe token.token
        msg.payload.title shouldBe "Asignación a servicio"
        msg.payload.body shouldContain "Vocalista"
        msg.payload.body shouldContain "Domingo de Adoración"
        msg.payload.channelId shouldBe "services"
        msg.payload.category shouldBe "SERVICE_ASSIGNMENT"
        msg.payload.data["type"] shouldBe "SERVICE_INVITATION"

        verify {
            ctx.notificationAppService.sendNotification(
                userId = memberId,
                title = any(),
                message = any(),
                type = NotificationType.SERVICE_INVITATION,
                relatedEntityId = any(),
                relatedEntityType = eq("SERVICE")
            )
        }
    }
})
