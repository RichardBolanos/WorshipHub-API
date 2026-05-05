package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.DevicePlatform
import com.worshiphub.domain.collaboration.push.DeviceToken
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import java.time.LocalDateTime
import java.util.*

class ApnsConfigE2ETest : FreeSpec({

    "18.23 E2E: push to iOS device includes correct payload with category" {
        val ctx = buildContext()
        val iosUser = UUID.randomUUID()

        val iosToken = DeviceToken(
            userId = iosUser,
            token = "ios-apns-token-12345678",
            platform = DevicePlatform.IOS
        )
        every { ctx.deviceTokenRepo.findByUserId(iosUser) } returns listOf(iosToken)
        every { ctx.deviceTokenRepo.deleteByToken(any()) } just Runs
        ctx.enableAllPreferences(iosUser)

        val scheduledDate = LocalDateTime.now().plusDays(3)
        val event = PushEvent.ServiceAssignment(
            recipientUserIds = listOf(iosUser),
            serviceName = "Servicio Dominical",
            scheduledDate = scheduledDate,
            roles = mapOf(iosUser to "Pianista")
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.token shouldBe "ios-apns-token-12345678"
        msg.payload.category shouldBe "SERVICE_ASSIGNMENT"
        msg.payload.channelId shouldBe "services"
        msg.payload.sound shouldBe "default"
    }
})
