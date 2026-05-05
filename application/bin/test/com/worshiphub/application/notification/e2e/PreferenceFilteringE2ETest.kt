package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.verify
import java.util.*

class PreferenceFilteringE2ETest : FreeSpec({

    "18.21 E2E: preference filtering — in-app stored but push skipped when preference disabled" {
        val ctx = buildContext()
        val userId = UUID.randomUUID()
        val teamId = UUID.randomUUID()

        ctx.registerToken(userId, "token-user")
        ctx.disablePreference(userId, teamChanges = false)

        val event = PushEvent.ChatMessage(
            recipientUserIds = listOf(userId),
            senderName = "Carlos",
            teamName = "Equipo",
            messageExcerpt = "Hola equipo",
            teamId = teamId
        )

        ctx.service.processPushEvent(event)

        // In-app notification IS stored (always for users passing role filter)
        verify {
            ctx.notificationAppService.sendNotification(
                userId = userId,
                title = any(),
                message = any(),
                type = NotificationType.TEAM_ASSIGNMENT,
                relatedEntityId = any(),
                relatedEntityType = any()
            )
        }

        // Push NOT sent (preference disabled)
        ctx.gateway.sentMessages.shouldBeEmpty()
    }
})
