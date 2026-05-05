package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.*

class ChatMessageE2ETest : FreeSpec({

    "18.3 E2E: chat message sends push to all team members except sender" {
        val ctx = buildContext()
        val member1 = UUID.randomUUID()
        val member2 = UUID.randomUUID()
        val teamId = UUID.randomUUID()

        ctx.registerToken(member1, "token-member1")
        ctx.registerToken(member2, "token-member2")
        ctx.enableAllPreferences(member1)
        ctx.enableAllPreferences(member2)

        val event = PushEvent.ChatMessage(
            recipientUserIds = listOf(member1, member2),
            senderName = "Carlos",
            teamName = "Equipo de Alabanza",
            messageExcerpt = "¿Ensayamos el sábado?",
            teamId = teamId
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 2
        ctx.gateway.findByToken("token-member1") shouldHaveSize 1
        ctx.gateway.findByToken("token-member2") shouldHaveSize 1

        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Carlos en Equipo de Alabanza"
        msg.payload.body shouldBe "¿Ensayamos el sábado?"
        msg.payload.channelId shouldBe "chat"
        msg.payload.data["type"] shouldBe "CHAT_MESSAGE"
        msg.payload.data["silent"] shouldBe "true"
    }
})
