package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class NewSongE2ETest : FreeSpec({

    "18.7 E2E: new song sends push to active church members except creator" {
        val ctx = buildContext()
        val member1 = UUID.randomUUID()
        val member2 = UUID.randomUUID()
        val songId = UUID.randomUUID()

        ctx.registerToken(member1, "token-m1")
        ctx.registerToken(member2, "token-m2")
        ctx.enableAllPreferences(member1)
        ctx.enableAllPreferences(member2)

        val event = PushEvent.NewSong(
            recipientUserIds = listOf(member1, member2),
            songTitle = "Reckless Love",
            artist = "Cory Asbury",
            addedByName = "Carlos",
            songId = songId
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 2
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Nueva canción agregada"
        msg.payload.body shouldContain "Reckless Love"
        msg.payload.body shouldContain "Cory Asbury"
        msg.payload.body shouldContain "Carlos"
        msg.payload.channelId shouldBe "songs"
        msg.payload.data["type"] shouldBe "NEW_SONG"
        msg.payload.data["entityId"] shouldBe songId.toString()
    }
})
