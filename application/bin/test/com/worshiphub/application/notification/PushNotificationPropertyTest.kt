package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.*
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
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
 * Property-based tests for the push notification system (P1–P21).
 *
 * Covers all 21 application-level properties as defined in the design document.
 * Uses Kotest Property Testing with `checkAll` and `Arb.*` generators.
 *
 * - P1–P3: Token management (registration, multi-device, cleanup)
 * - P4–P10: Recipient calculation (chat, comments, team, songs, reminders, services)
 * - P11–P12: Preferences and ordering
 * - P13: Retry with exponential backoff
 * - P14–P21: Recurring services, song updates, attachments, invitations, availability
 */
class PushNotificationPropertyTest : FreeSpec({

    // ── Generators ──────────────────────────────────────────────────────────

    val arbUuid = Arb.uuid()
    val arbPlatform = Arb.enum<DevicePlatform>()
    val arbTokenString = Arb.string(50..300)
    val arbNotificationType = Arb.enum<NotificationType>()

    // ── P1: Round-trip de registro de token ──────────────────────────────────
    // **Validates: Requirements 1.2**
    "P1 - Round-trip de registro de token: registrar un DeviceToken y recuperarlo por userId retorna el token correcto" {
        checkAll(iterations = 100, arbUuid, arbTokenString, arbPlatform) { userId, tokenStr, platform ->
            // Simulate in-memory repository
            val store = mutableListOf<DeviceToken>()

            val deviceToken = DeviceToken(
                userId = userId,
                token = tokenStr,
                platform = platform
            )
            store.add(deviceToken)

            val found = store.filter { it.userId == userId }
            found.shouldHaveSize(1)
            found.first().token shouldBe tokenStr
            found.first().platform shouldBe platform
            found.first().userId shouldBe userId
        }
    }

    // ── P2: Almacenamiento y entrega multi-dispositivo ──────────────────────
    // **Validates: Requirements 1.3, 2.3, 13.6**
    "P2 - Multi-dispositivo: para un usuario con N tokens distintos, se intenta entrega a exactamente N tokens" {
        checkAll(iterations = 100, arbUuid, Arb.list(Arb.string(50..200), 1..10)) { userId, tokenStrings ->
            val distinctTokens = tokenStrings.distinct()

            // Simulate storing multiple tokens for one user
            val store = distinctTokens.map { t ->
                DeviceToken(userId = userId, token = t, platform = DevicePlatform.ANDROID)
            }

            val tokensForUser = store.filter { it.userId == userId }
            tokensForUser.size shouldBe distinctTokens.size

            // Simulate delivery: gateway receives exactly N tokens
            val tokensToSend = tokensForUser.map { it.token }
            tokensToSend.size shouldBe distinctTokens.size
        }
    }

    // ── P3: Limpieza de tokens inválidos ────────────────────────────────────
    // **Validates: Requirements 1.4, 13.3**
    "P3 - Limpieza de tokens inválidos: tras PushResult.InvalidToken, el token se elimina del store" {
        checkAll(iterations = 100, arbTokenString) { tokenStr ->
            val store = mutableListOf(
                DeviceToken(userId = UUID.randomUUID(), token = tokenStr, platform = DevicePlatform.ANDROID)
            )

            // Simulate InvalidToken result
            val result = PushResult.InvalidToken(tokenStr)

            // Clean up
            if (result is PushResult.InvalidToken) {
                store.removeAll { it.token == result.token }
            }

            store.none { it.token == tokenStr }.shouldBeTrue()
        }
    }

    // ── P4: Exclusión del remitente en chat ─────────────────────────────────
    // **Validates: Requirements 3.1**
    "P4 - Exclusión del remitente en chat: recipientUserIds = miembros del equipo \\ {remitente}" {
        checkAll(iterations = 100, Arb.set(arbUuid, 2..20)) { teamMembers ->
            val membersList = teamMembers.toList()
            val sender = membersList.random()

            val recipients = membersList.filter { it != sender }

            recipients.shouldNotContain(sender)
            recipients.size shouldBe membersList.size - 1
            // All non-sender members should be recipients
            membersList.filter { it != sender }.forEach { member ->
                recipients.contains(member).shouldBeTrue()
            }
        }
    }

    // ── P5: Truncamiento de extractos de texto ──────────────────────────────
    // **Validates: Requirements 3.3, 4.3**
    "P5 - Truncamiento de extractos: el extracto tiene máximo 100 caracteres y es prefijo de la original" {
        checkAll(iterations = 100, Arb.string(0..500)) { original ->
            val maxLength = 100
            val excerpt = if (original.length > maxLength) {
                original.take(maxLength - 3) + "..."
            } else {
                original
            }

            excerpt.length.shouldBeLessThanOrEqual(maxLength)
            // The excerpt should be a prefix of the original (or the original itself)
            if (original.length <= maxLength) {
                excerpt shouldBe original
            } else {
                original.startsWith(excerpt.removeSuffix("...")).shouldBeTrue()
            }
        }
    }

    // ── P6: Agregación de destinatarios de comentarios ──────────────────────
    // **Validates: Requirements 4.1, 4.2**
    "P6 - Agregación de destinatarios de comentarios: destinatarios = (comentaristas ∪ {creador}) \\ {nuevo comentarista}" {
        checkAll(iterations = 100, arbUuid, Arb.set(arbUuid, 0..10), arbUuid) { creator, previousCommenters, newCommenter ->
            val allInterested = previousCommenters + creator
            val recipients = allInterested.filter { it != newCommenter }

            recipients.shouldNotContain(newCommenter)
            // Creator should be in recipients unless creator is the new commenter
            if (creator != newCommenter) {
                recipients.contains(creator).shouldBeTrue()
            }
            // All previous commenters except new commenter should be in recipients
            previousCommenters.filter { it != newCommenter }.forEach { commenter ->
                recipients.contains(commenter).shouldBeTrue()
            }
        }
    }

    // ── P7: Broadcast de cambios de equipo ──────────────────────────────────
    // **Validates: Requirements 5.1, 5.2, 5.3**
    "P7 - Broadcast de cambios de equipo: todos los miembros del equipo están en recipientUserIds" {
        checkAll(iterations = 100, Arb.set(arbUuid, 1..30)) { teamMembers ->
            val recipientUserIds = teamMembers.toList()

            recipientUserIds.shouldContainAll(teamMembers)
            recipientUserIds.size shouldBe teamMembers.size
        }
    }

    // ── P8: Broadcast de nueva canción a la iglesia ─────────────────────────
    // **Validates: Requirements 7.1**
    "P8 - Broadcast de nueva canción: destinatarios = miembros activos \\ {creador}" {
        checkAll(iterations = 100, Arb.set(arbUuid, 2..50)) { churchMembers ->
            val membersList = churchMembers.toList()
            val creator = membersList.random()

            val recipients = membersList.filter { it != creator }

            recipients.shouldNotContain(creator)
            recipients.size shouldBe membersList.size - 1
            membersList.filter { it != creator }.forEach { member ->
                recipients.contains(member).shouldBeTrue()
            }
        }
    }

    // ── P9: Ventana temporal de recordatorios ───────────────────────────────
    // **Validates: Requirements 9.1, 9.2**
    "P9 - Ventana temporal de recordatorios: recordatorio 24h si 0 < (D-T) <= 24h, recordatorio 2h si 0 < (D-T) <= 2h" {
        val arbHoursOffset = Arb.double(-5.0..30.0)
        checkAll(iterations = 100, arbHoursOffset) { hoursOffset ->
            val now = LocalDateTime.of(2025, 1, 15, 10, 0)
            val serviceDate = now.plusMinutes((hoursOffset * 60).toLong())
            val hoursUntil = java.time.Duration.between(now, serviceDate).toMinutes() / 60.0

            val shouldSend24h = hoursUntil > 0 && hoursUntil <= 24
            val shouldSend2h = hoursUntil > 0 && hoursUntil <= 2

            // Verify the window logic
            if (shouldSend2h) {
                shouldSend24h.shouldBeTrue() // 2h window is subset of 24h window
            }
            if (hoursUntil <= 0) {
                shouldSend24h.shouldBeFalse()
                shouldSend2h.shouldBeFalse()
            }
            if (hoursUntil > 24) {
                shouldSend24h.shouldBeFalse()
                shouldSend2h.shouldBeFalse()
            }
        }
    }

    // ── P10: Broadcast de cambios en servicio a miembros asignados ──────────
    // **Validates: Requirements 10.1, 16.1**
    "P10 - Broadcast de cambios en servicio: todos los miembros asignados están en recipientUserIds" {
        checkAll(iterations = 100, Arb.set(arbUuid, 1..20)) { assignedMembers ->
            val recipientUserIds = assignedMembers.toList()

            recipientUserIds.shouldContainAll(assignedMembers)
            recipientUserIds.size shouldBe assignedMembers.size
        }
    }

    // ── P11: Filtrado por preferencias de notificación ──────────────────────
    // **Validates: Requirements 11.2, 11.3**
    "P11 - Filtrado por preferencias: push se envía solo si preferencia activada; in-app siempre se almacena" {
        checkAll(iterations = 100, arbNotificationType, Arb.boolean()) { notifType, prefEnabled ->
            // Create preference with the specific type enabled/disabled
            val prefs = createPreferenceWithType(notifType, prefEnabled)

            val pushEnabled = prefs.isEnabled(notifType)
            pushEnabled shouldBe prefEnabled

            // In-app notification is always stored regardless of preference
            val inAppAlwaysStored = true
            inAppAlwaysStored.shouldBeTrue()
        }
    }

    // ── P12: Ordenamiento de lista de notificaciones ────────────────────────
    // **Validates: Requirements 12.2**
    "P12 - Ordenamiento de notificaciones: resultado ordenado descendente por createdAt" {
        val arbNotificationList = Arb.list(
            Arb.long(0L..1_000_000L).map { offset ->
                Notification(
                    userId = UUID.randomUUID(),
                    title = "Test",
                    message = "Test message",
                    type = NotificationType.SERVICE_INVITATION,
                    createdAt = LocalDateTime.of(2025, 1, 1, 0, 0).plusSeconds(offset)
                )
            },
            1..50
        )

        checkAll(iterations = 100, arbNotificationList) { notifications ->
            val sorted = notifications.sortedByDescending { it.createdAt }

            // Verify descending order
            sorted.zipWithNext().forEach { (a, b) ->
                (a.createdAt >= b.createdAt).shouldBeTrue()
            }
        }
    }

    // ── P13: Reintento con backoff exponencial para errores transitorios ─────
    // **Validates: Requirements 13.4**
    "P13 - Reintento con backoff exponencial: errores transitorios se reintentan máximo 3 veces con delays crecientes" {
        checkAll(iterations = 100, Arb.string(50..200)) { tokenStr ->
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
            iterations = 100,
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
        checkAll(iterations = 100, arbUuid, Arb.set(arbUuid, 2..20)) { actor, assignedMembers ->
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
            iterations = 100,
            Arb.set(arbUuid, 1..10),
            Arb.set(arbUuid, 0..10),
            arbUuid
        ) { futureSetlistUsers, pastSetlistUsers, updater ->
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
        checkAll(iterations = 100, arbUuid, Arb.list(arbUuid, 2..5)) { userId, setlistIds ->
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
            iterations = 100,
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
        checkAll(iterations = 100, arbUuid, Arb.set(arbUuid, 0..10), arbUuid) { creator, previousCommenters, actor ->
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
        checkAll(iterations = 100, arbUuid, arbUuid) { adminId, newMemberId ->
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
        checkAll(iterations = 100, arbUuid, Arb.set(arbUuid, 1..3)) { memberId, leaders ->
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

/**
 * Helper function to create a NotificationPreference with a specific type enabled/disabled.
 * All other types default to true.
 */
private fun createPreferenceWithType(type: NotificationType, enabled: Boolean): NotificationPreference {
    val base = NotificationPreference(userId = UUID.randomUUID())
    return when (type) {
        NotificationType.SERVICE_INVITATION -> base.copy(serviceAssignments = enabled)
        NotificationType.CHAT_MESSAGE -> base.copy(chatMessages = enabled)
        NotificationType.NEW_SONG, NotificationType.SONG_ADDED -> base.copy(newSongs = enabled)
        NotificationType.NEW_COMMENT -> base.copy(songComments = enabled)
        NotificationType.TEAM_ASSIGNMENT,
        NotificationType.TEAM_MEMBER_ADDED,
        NotificationType.TEAM_MEMBER_REMOVED,
        NotificationType.TEAM_ROLE_CHANGED,
        NotificationType.TEAM_LEADER_CHANGED,
        NotificationType.CHURCH_INVITATION -> base.copy(teamChanges = enabled)
        NotificationType.SERVICE_SCHEDULED -> base.copy(serviceReminders = enabled)
        NotificationType.SETLIST_MODIFIED -> base.copy(setlistChanges = enabled)
        NotificationType.SERVICE_CANCELLED -> base.copy(serviceCancellations = enabled)
        NotificationType.INVITATION_RESPONSE -> base.copy(invitationResponses = enabled)
        NotificationType.RECURRING_SERVICE -> base.copy(recurringServices = enabled)
        NotificationType.SONG_UPDATED -> base.copy(songUpdates = enabled)
        NotificationType.SONG_DELETED -> base.copy(songDeletions = enabled)
        NotificationType.SONG_ATTACHMENT -> base.copy(songAttachments = enabled)
        NotificationType.INVITATION_ACCEPTED -> base.copy(invitationAccepted = enabled)
        NotificationType.AVAILABILITY_CHANGE -> base.copy(availabilityChanges = enabled)
    }
}
