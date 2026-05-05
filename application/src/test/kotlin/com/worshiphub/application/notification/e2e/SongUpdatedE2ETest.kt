package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class SongUpdatedE2ETest : FreeSpec({

    "18.15 E2E: song updated sends push only to users with future setlists" {
        val ctx = buildContext()
        val userWithFutureSetlist = UUID.randomUUID()
        val songId = UUID.randomUUID()

        ctx.registerToken(userWithFutureSetlist, "token-future")
        ctx.enableAllPreferences(userWithFutureSetlist)

        val event = PushEvent.SongUpdated(
            recipientUserIds = listOf(userWithFutureSetlist),
            songTitle = "Way Maker",
            changedFields = listOf("key", "bpm"),
            updatedByName = "Carlos",
            songId = songId
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Canción actualizada"
        msg.payload.body shouldContain "Way Maker"
        msg.payload.body shouldContain "key, bpm"
        msg.payload.body shouldContain "Carlos"
        msg.payload.channelId shouldBe "songs"
        msg.payload.data["type"] shouldBe "SONG_UPDATED"
        msg.payload.data["entityId"] shouldBe songId.toString()
    }
})
