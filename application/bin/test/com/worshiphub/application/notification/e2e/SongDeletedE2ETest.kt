package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushEvent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class SongDeletedE2ETest : FreeSpec({

    "18.16 E2E: song deleted sends push with affected setlists" {
        val ctx = buildContext()
        val member = UUID.randomUUID()

        ctx.registerToken(member, "token-member")
        ctx.enableAllPreferences(member)

        val event = PushEvent.SongDeleted(
            recipientUserIds = listOf(member),
            songTitle = "Oceans",
            deletedByName = "Ana",
            affectedSetlists = listOf("Setlist Domingo", "Setlist Miércoles")
        )

        ctx.service.processPushEvent(event)

        ctx.gateway.sentMessages shouldHaveSize 1
        val msg = ctx.gateway.sentMessages.first()
        msg.payload.title shouldBe "Canción eliminada"
        msg.payload.body shouldContain "Oceans"
        msg.payload.body shouldContain "Ana"
        msg.payload.body shouldContain "Setlist Domingo"
        msg.payload.body shouldContain "Setlist Miércoles"
        msg.payload.channelId shouldBe "songs"
        msg.payload.data["type"] shouldBe "SONG_DELETED"
    }
})
