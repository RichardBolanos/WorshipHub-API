package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class SongCommentE2ETest : FreeSpec({

    "18.4 E2E: song comment sends push to creator + previous commenters minus actor" {
        val ctx = buildContext()
        val creator = UUID.randomUUID()
        val prevCommenter = UUID.randomUUID()
        val songId = UUID.randomUUID()

        ctx.registerToken(creator, "token-creator")
        ctx.registerToken(prevCommenter, "token-prev")
        ctx.enableAllPreferences(creator)
        ctx.enableAllPreferences(prevCommenter)

        val event = PushEvent.SongComment(
            recipientUserIds = listOf(creator, prevCommenter),
            commenterName = "María",
            songTitle = "Grande es el Señor",
            commentExcerpt = "Me encanta esta versión, podríamos...",
            songId = songId
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 2
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldContain "Grande es el Señor"
        msg.payload.body shouldContain "María"
        msg.payload.channelId shouldBe "songs"
        msg.payload.data["type"] shouldBe "NEW_COMMENT"
        msg.payload.data["entityId"] shouldBe songId.toString()
    }
})
