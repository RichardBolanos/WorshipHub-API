package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class AttachmentAddedE2ETest : FreeSpec({

    "18.17 E2E: attachment added sends push to creator + commenters" {
        val ctx = buildContext()
        val creator = UUID.randomUUID()
        val commenter = UUID.randomUUID()
        val songId = UUID.randomUUID()

        ctx.registerToken(creator, "token-creator")
        ctx.registerToken(commenter, "token-commenter")
        ctx.enableAllPreferences(creator)
        ctx.enableAllPreferences(commenter)

        val event = PushEvent.AttachmentAdded(
            recipientUserIds = listOf(creator, commenter),
            songTitle = "10,000 Reasons",
            attachmentType = "YOUTUBE_LINK",
            addedByName = "Pedro",
            songId = songId
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 2
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Nuevo adjunto en canción"
        msg.payload.body shouldContain "10,000 Reasons"
        msg.payload.body shouldContain "YOUTUBE_LINK"
        msg.payload.body shouldContain "Pedro"
        msg.payload.channelId shouldBe "songs"
        msg.payload.data["type"] shouldBe "SONG_ATTACHMENT"
        msg.payload.data["entityId"] shouldBe songId.toString()
    }
})
