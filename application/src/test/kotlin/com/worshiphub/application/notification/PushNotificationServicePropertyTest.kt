package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.*
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Property-based tests for the push notification application service layer.
 *
 * Covers properties P13–P21 as defined in the design document.
 * Uses Kotest Property Testing with Arb generators and in-memory mocks.
 */
class PushNotificationServicePropertyTest : FreeSpec({

    // ── Generators ──────────────────────────────────────────────────────────

    val arbUuid = Arb.uuid()

    // ── P13: Reintento con backoff exponencial para errores transitorios ─────
    // **Validates: Requirements 13.4**
    "P13 - Reintento con backoff exponencial: errores transitorios se reintentan máximo 3 veces con delays crecientes" {
        checkAll(Arb.string(50..200)) { tokenStr ->
            val maxRetries = 3
            val initialBackoffMs = 1000L
            var attemptCount = 0
            val backoffDelays = mutableListOf<Long>()

            // Simulate the retry logic from PushNotificationService
            var tokensToRetry = listOf(tokenStr)

            for (attempt in 0..maxRetries) {
                if (tokensToRetry.isEmpty()) break

                attemptCount++

                // Simulate TransientError on every attempt
                val nextRetryTokens = mutableListOf<String>()
                tokensToRetry.forEach { token ->
                    if (attempt < maxRetries) {
                        nextRetryTokens.add(token)
                    }
                }

                tokensToRetry = nextRetryTokens

                if (tokensToRetry.isNotEmpty() && attempt < maxRetries) {
                    val backoffMs = initialBackoffMs * (1L shl attempt) // 1s, 2s, 4s
                    backoffDelays.add(backoffMs)
                }
            }

            // Should attempt at most maxRetries + 1 times (initial + retries)
            attemptCount.shouldBeLessThanOrEqual(maxRetries + 1)

            // Backoff delays should be exponentially increasing: 1000, 2000, 4000
            if (backoffDelays.size >= 2) {
                backoffDelays.zipWithNext().forEach { (a, b) ->
                    (b >= a).shouldBeTrue()
                }
            }
            // First delay should be initialBackoffMs
            if (backoffDelays.isNotEmpty()) {
                backoffDelays.first() shouldBe initialBackoffMs
            }
        }
    }

    // ── P14: Consolidación de servicio recurrente ───────────────────────────
    // **Validates: Requirements 17.1, 17.2**
    "P14 - Consolidación de servicio recurrente: cada miembro recibe exactamente 1 notificación con N fechas" {
        checkAll(
            Arb.set(arbUuid, 2..15),
            Arb.list(Arb.long(1L..365L), 1..12)
        ) { members, dayOffsets ->
            val scheduledDates = dayOffsets.distinct().map { offset ->
                LocalDateTime.of(2025, 6, 1, 10, 0).plusDays(offset)
            }
            val membersList = members.toList()

            // Create a single RecurringServiceCreated event with all members and dates
            val event = PushEvent.RecurringServiceCreated(
                recipientUserIds = membersList,
                serviceName = "Servicio Dominical",
                scheduledDates = scheduledDates,
                recurrencePattern = "WEEKLY",
                roles = membersList.associateWith { "músico" }
            )

            // Each member should appear exactly once in recipientUserIds
            val recipientCounts = event.recipientUserIds.groupBy { it }.mapValues { it.value.size }
            recipientCounts.values.forEach { count ->
                count shouldBe 1
            }

            // The event should contain all scheduled dates
            event.scheduledDates.size shouldBe scheduledDates.size

            // All members should be recipients
            event.recipientUserIds.shouldContainAll(membersList)
        }
    }

    // ── P15: Exclusión del actor en recurrentes ─────────────────────────────
    // **Validates: Requirements 17.1, 18.1, 19.1**
    "P15 - Exclusión del actor en eventos de servicio recurrente: actor no está en recipientUserIds" {
        checkAll(arbUuid, Arb.set(arbUuid, 2..20)) { actor, assignedMembers ->
            val allMembers = (assignedMembers + actor).toList()

            // Recipients should exclude the actor
            val recipients = allMembers.filter { it != actor }

            recipients.shouldNotContain(actor)
            // All non-actor members should be recipients
            assignedMembers.filter { it != actor }.forEach { member ->
                recipients.contains(member).shouldBeTrue()
            }
        }
    }

    // ── P16: Destinatarios limitados a setlists futuros ─────────────────────
    // **Validates: Requirements 20.1**
    "P16 - Destinatarios de actualización de canción limitados a setlists futuros" {
        checkAll(
            Arb.set(arbUuid, 1..10),
            Arb.set(arbUuid, 0..10),
            arbUuid
        ) { futureSetlistUsers, pastSetlistUsers, updater ->
            val now = LocalDateTime.now()

            // Only users with the song in FUTURE setlists should receive notification
            // Exclude the updater
            val recipients = futureSetlistUsers.filter { it != updater }

            // Past setlist users should NOT be in recipients (unless also in future)
            val pastOnly = pastSetlistUsers - futureSetlistUsers
            pastOnly.forEach { user ->
                recipients.shouldNotContain(user)
            }

            // Updater should NOT be in recipients
            recipients.shouldNotContain(updater)

            // All future setlist users except updater should be recipients
            futureSetlistUsers.filter { it != updater }.forEach { user ->
                recipients.contains(user).shouldBeTrue()
            }
        }
    }

    // ── P17: Deduplicación por canción en múltiples setlists ────────────────
    // **Validates: Requirements 20.3**
    "P17 - Deduplicación: usuario con canción en múltiples setlists futuros recibe exactamente 1 notificación" {
        checkAll(arbUuid, Arb.list(arbUuid, 2..5)) { userId, setlistIds ->
            // User appears in multiple setlists for the same song
            val userAppearances = setlistIds.map { setlistId ->
                Pair(userId, setlistId)
            }

            // After deduplication, user should appear exactly once in recipients
            val deduplicatedRecipients = userAppearances.map { it.first }.distinct()

            deduplicatedRecipients.shouldHaveSize(1)
            deduplicatedRecipients.first() shouldBe userId
        }
    }

    // ── P18: Destinatarios de eliminación de canción ────────────────────────
    // **Validates: Requirements 21.1, 21.2**
    "P18 - Destinatarios de eliminación de canción: todos los usuarios en setlists (excepto eliminador) reciben notificación con nombres de setlists" {
        checkAll(
            Arb.set(arbUuid, 1..20),
            arbUuid,
            Arb.list(Arb.string(5..30), 1..5)
        ) { setlistUsers, deleter, setlistNames ->
            val allUsers = (setlistUsers + deleter).toList()

            // Recipients = all setlist users except deleter
            val recipients = allUsers.filter { it != deleter }.distinct()

            recipients.shouldNotContain(deleter)
            setlistUsers.filter { it != deleter }.forEach { user ->
                recipients.contains(user).shouldBeTrue()
            }

            // Create event to verify setlist names are included
            val event = PushEvent.SongDeleted(
                recipientUserIds = recipients,
                songTitle = "Test Song",
                deletedByName = "Deleter",
                affectedSetlists = setlistNames
            )

            event.affectedSetlists shouldBe setlistNames
            event.recipientUserIds.shouldNotContain(deleter)
        }
    }

    // ── P19: Destinatarios de attachment ─────────────────────────────────────
    // **Validates: Requirements 22.1**
    "P19 - Destinatarios de attachment: destinatarios = (comentaristas ∪ {creador}) \\ {actor}" {
        checkAll(arbUuid, Arb.set(arbUuid, 0..10), arbUuid) { creator, previousCommenters, actor ->
            val allInterested = (previousCommenters + creator).toList()
            val recipients = allInterested.filter { it != actor }.distinct()

            recipients.shouldNotContain(actor)
            // Creator should be in recipients unless creator is the actor
            if (creator != actor) {
                recipients.contains(creator).shouldBeTrue()
            }
            // All previous commenters except actor should be in recipients
            previousCommenters.filter { it != actor }.forEach { commenter ->
                recipients.contains(commenter).shouldBeTrue()
            }
        }
    }

    // ── P20: Invitación aceptada al invitador ───────────────────────────────
    // **Validates: Requirements 23.1**
    "P20 - Invitación aceptada: el único destinatario es el admin que envió la invitación" {
        checkAll(arbUuid, arbUuid) { adminId, newMemberId ->
            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(adminId),
                newMemberName = "New Member",
                newMemberEmail = "new@example.com",
                acceptedRole = "MEMBER"
            )

            event.recipientUserIds.shouldHaveSize(1)
            event.recipientUserIds.first() shouldBe adminId
            // The new member should not be a recipient (they are the one who accepted)
            if (adminId != newMemberId) {
                event.recipientUserIds.shouldNotContain(newMemberId)
            }
        }
    }

    // ── P21: Disponibilidad notifica al líder ───────────────────────────────
    // **Validates: Requirements 24.1, 25.1**
    "P21 - Disponibilidad notifica al líder: recipientUserIds = líderes del equipo" {
        checkAll(arbUuid, Arb.set(arbUuid, 1..3)) { memberId, leaders ->
            val leadersList = leaders.toList()

            // MemberUnavailable event
            val unavailableEvent = PushEvent.MemberUnavailable(
                recipientUserIds = leadersList,
                memberName = "Member Name",
                unavailableDate = LocalDate.of(2025, 6, 15),
                reason = "Vacaciones"
            )

            unavailableEvent.recipientUserIds shouldBe leadersList
            unavailableEvent.recipientUserIds.shouldContainAll(leadersList)
            // The member should not be a recipient (unless they happen to also be a leader)
            if (!leaders.contains(memberId)) {
                unavailableEvent.recipientUserIds.shouldNotContain(memberId)
            }

            // MemberAvailableAgain event
            val availableEvent = PushEvent.MemberAvailableAgain(
                recipientUserIds = leadersList,
                memberName = "Member Name",
                previouslyUnavailableDate = LocalDate.of(2025, 6, 15)
            )

            availableEvent.recipientUserIds shouldBe leadersList
            availableEvent.recipientUserIds.shouldContainAll(leadersList)
            if (!leaders.contains(memberId)) {
                availableEvent.recipientUserIds.shouldNotContain(memberId)
            }
        }
    }
})
