package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class TeamMemberChangeE2ETest : FreeSpec({

    "18.5 E2E: team member change sends push to existing members" {
        val ctx = buildContext()
        val existingMember = UUID.randomUUID()
        val teamId = UUID.randomUUID()

        ctx.registerToken(existingMember, "token-existing")
        ctx.enableAllPreferences(existingMember)

        val event = PushEvent.TeamMemberChange(
            recipientUserIds = listOf(existingMember),
            teamName = "Equipo de Alabanza",
            changeDescription = "Pedro fue agregado como Guitarrista",
            teamId = teamId,
            notificationType = NotificationType.TEAM_MEMBER_ADDED
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldContain "Equipo de Alabanza"
        msg.payload.body shouldContain "Pedro"
        msg.payload.channelId shouldBe "team"
        msg.payload.data["type"] shouldBe "TEAM_MEMBER_ADDED"
        msg.payload.data["entityId"] shouldBe teamId.toString()
    }
})
