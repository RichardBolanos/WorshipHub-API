package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.push.*
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for push notification payloads and recipients per notification type.
 * Validates: Requirements 2.1–25.1, 29.4, 29.5, 29.6
 *
 * Each test section verifies:
 * - Payload content (title, body, data, channelId, category)
 * - Correct recipients
 * - Edge cases per notification type
 */
class PushNotificationPayloadTest : FreeSpec({

    /**
     * Creates a fully-mocked PushNotificationService test context.
     * All users default to ADMIN role so they pass role filtering.
     * Preferences default to all-enabled.
     */
    fun createContext(): TestCtx {
        val pushGateway = mockk<PushGateway>(relaxed = true)
        val deviceTokenRepo = mockk<DeviceTokenRepository>()
        val prefRepo = mockk<NotificationPreferenceRepository>()
        val notifAppService = mockk<NotificationApplicationService>(relaxed = true)
        val userRoleResolver = mockk<UserRoleResolver>()

        val service = PushNotificationService(
            pushGateway, deviceTokenRepo, prefRepo, notifAppService, userRoleResolver
        )

        return TestCtx(service, pushGateway, deviceTokenRepo, prefRepo, notifAppService, userRoleResolver)
    }

    /**
     * Configures a user to pass role filtering, have default preferences, and have a device token.
     * Returns the captured PushPayload slot for assertion.
     */
    fun TestCtx.setupUser(userId: UUID, tokenStr: String = "fcm-token-${userId.toString().take(8)}"): CapturingSlot<PushPayload> {
        every { userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
        every { prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(userId = userId)
        every { deviceTokenRepo.findByUserId(userId) } returns listOf(
            DeviceToken(userId = userId, token = tokenStr, platform = DevicePlatform.ANDROID)
        )
        val payloadSlot = slot<PushPayload>()
        every { pushGateway.sendToDevices(any(), capture(payloadSlot)) } returns listOf(PushResult.Success("msg-ok"))
        return payloadSlot
    }

    /**
     * Configures multiple users and returns a mutable list that captures all payloads sent.
     */
    fun TestCtx.setupUsers(userIds: List<UUID>): MutableList<PushPayload> {
        val capturedPayloads = mutableListOf<PushPayload>()
        userIds.forEach { userId ->
            every { userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            every { prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(userId = userId)
            every { deviceTokenRepo.findByUserId(userId) } returns listOf(
                DeviceToken(userId = userId, token = "fcm-token-${userId.toString().take(8)}", platform = DevicePlatform.ANDROID)
            )
        }
        every { pushGateway.sendToDevices(any(), capture(capturedPayloads)) } returns listOf(PushResult.Success("msg-ok"))
        return capturedPayloads
    }


    // =========================================================================
    // 16.1 — Payload de asignación a servicio (R2)
    // =========================================================================
    "16.1 ServiceAssignment payload" - {

        "payload contains service name, scheduled date and assigned role" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.ServiceAssignment(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio Dominical",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                roles = mapOf(memberId to "Guitarrista")
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Guitarrista"
            payloadSlot.captured.body shouldContain "Servicio Dominical"
            payloadSlot.captured.body shouldContain "15/06/2025"
        }

        "recipients are the assigned members" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.ServiceAssignment(
                recipientUserIds = listOf(member1, member2),
                serviceName = "Servicio",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                roles = mapOf(member1 to "Vocalista", member2 to "Baterista")
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${member1.toString().take(8)}"), any()) }
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${member2.toString().take(8)}"), any()) }
        }

        "notification includes channelId services and category SERVICE_ASSIGNMENT" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.ServiceAssignment(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                roles = mapOf(memberId to "Pianista")
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.channelId shouldBe "services"
            payloadSlot.captured.category shouldBe "SERVICE_ASSIGNMENT"
        }
    }

    // =========================================================================
    // 16.2 — Payload de mensaje de chat (R3)
    // =========================================================================
    "16.2 ChatMessage payload" - {

        "payload contains sender name, team name and message excerpt" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.ChatMessage(
                recipientUserIds = listOf(recipientId),
                senderName = "María García",
                teamName = "Equipo Alabanza",
                messageExcerpt = "Hola equipo, recuerden el ensayo",
                teamId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.title shouldContain "María García"
            payloadSlot.captured.title shouldContain "Equipo Alabanza"
            payloadSlot.captured.body shouldBe "Hola equipo, recuerden el ensayo"
        }

        "recipients are team members except sender" {
            val ctx = createContext()
            val senderId = UUID.randomUUID()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            // recipientUserIds should already exclude sender (done by the publishing service)
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.ChatMessage(
                recipientUserIds = listOf(member1, member2),
                senderName = "Sender",
                teamName = "Team",
                messageExcerpt = "Hello",
                teamId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${member1.toString().take(8)}"), any()) }
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${member2.toString().take(8)}"), any()) }
            // Sender should NOT be in recipients
            verify(exactly = 0) { ctx.deviceTokenRepo.findByUserId(senderId) }
        }

        "excerpt is passed as-is (truncation is done by the publishing service)" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)
            val longExcerpt = "A".repeat(100)

            val event = PushEvent.ChatMessage(
                recipientUserIds = listOf(recipientId),
                senderName = "Sender",
                teamName = "Team",
                messageExcerpt = longExcerpt,
                teamId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldBe longExcerpt
            payloadSlot.captured.body.length shouldBe 100
        }
    }

    // =========================================================================
    // 16.3 — Payload de comentario en canción (R4)
    // =========================================================================
    "16.3 SongComment payload" - {

        "payload contains commenter name, song title and comment excerpt" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.SongComment(
                recipientUserIds = listOf(recipientId),
                commenterName = "Juan Pérez",
                songTitle = "Grande es el Señor",
                commentExcerpt = "Excelente arreglo en el puente",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.title shouldContain "Grande es el Señor"
            payloadSlot.captured.body shouldContain "Juan Pérez"
            payloadSlot.captured.body shouldContain "Excelente arreglo en el puente"
        }

        "recipients are creator plus previous commenters minus current commenter" {
            val ctx = createContext()
            val creator = UUID.randomUUID()
            val prevCommenter = UUID.randomUUID()
            // recipientUserIds already computed by publishing service: creator + prev commenters - actor
            ctx.setupUsers(listOf(creator, prevCommenter))

            val event = PushEvent.SongComment(
                recipientUserIds = listOf(creator, prevCommenter),
                commenterName = "Actor",
                songTitle = "Song",
                commentExcerpt = "Nice",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${creator.toString().take(8)}"), any()) }
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${prevCommenter.toString().take(8)}"), any()) }
        }
    }

    // =========================================================================
    // 16.4 — Payload de cambios en equipo (R5)
    // =========================================================================
    "16.4 TeamMemberChange payload" - {

        "payload for new member contains name and role" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.TeamMemberChange(
                recipientUserIds = listOf(recipientId),
                teamName = "Equipo Alabanza",
                changeDescription = "Carlos López se unió como Vocalista",
                teamId = UUID.randomUUID(),
                notificationType = NotificationType.TEAM_MEMBER_ADDED
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.title shouldContain "Equipo Alabanza"
            payloadSlot.captured.body shouldContain "Carlos López"
            payloadSlot.captured.body shouldContain "Vocalista"
        }

        "payload for removed member notifies remaining members" {
            val ctx = createContext()
            val remaining1 = UUID.randomUUID()
            val remaining2 = UUID.randomUUID()
            ctx.setupUsers(listOf(remaining1, remaining2))

            val event = PushEvent.TeamMemberChange(
                recipientUserIds = listOf(remaining1, remaining2),
                teamName = "Equipo",
                changeDescription = "Ana fue removida del equipo",
                teamId = UUID.randomUUID(),
                notificationType = NotificationType.TEAM_MEMBER_REMOVED
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${remaining1.toString().take(8)}"), any()) }
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${remaining2.toString().take(8)}"), any()) }
        }

        "payload for leader change notifies all members" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.TeamMemberChange(
                recipientUserIds = listOf(member1, member2),
                teamName = "Equipo",
                changeDescription = "Pedro es el nuevo líder del equipo",
                teamId = UUID.randomUUID(),
                notificationType = NotificationType.TEAM_LEADER_CHANGED
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "payload for role change notifies affected member" {
            val ctx = createContext()
            val affectedMember = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(affectedMember)

            val event = PushEvent.TeamMemberChange(
                recipientUserIds = listOf(affectedMember),
                teamName = "Equipo",
                changeDescription = "Tu rol cambió a Bajista",
                teamId = UUID.randomUUID(),
                notificationType = NotificationType.TEAM_ROLE_CHANGED
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Bajista"
        }
    }


    // =========================================================================
    // 16.5 — Payload de respuesta a invitación de servicio (R6)
    // =========================================================================
    "16.5 InvitationResponse payload" - {

        "payload contains member name, service name, date and accepted status" {
            val ctx = createContext()
            val leaderId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(leaderId)

            val event = PushEvent.InvitationResponse(
                recipientUserIds = listOf(leaderId),
                memberName = "Carlos López",
                serviceName = "Servicio Dominical",
                scheduledDate = LocalDateTime.of(2025, 7, 20, 9, 30),
                accepted = true
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Carlos López"
            payloadSlot.captured.body shouldContain "aceptó"
            payloadSlot.captured.body shouldContain "Servicio Dominical"
            payloadSlot.captured.body shouldContain "20/07/2025"
        }

        "payload shows declined when member declines" {
            val ctx = createContext()
            val leaderId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(leaderId)

            val event = PushEvent.InvitationResponse(
                recipientUserIds = listOf(leaderId),
                memberName = "Ana Martínez",
                serviceName = "Servicio Nocturno",
                scheduledDate = LocalDateTime.of(2025, 7, 20, 19, 0),
                accepted = false
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Ana Martínez"
            payloadSlot.captured.body shouldContain "declinó"
        }

        "recipient is the team leader" {
            val ctx = createContext()
            val leaderId = UUID.randomUUID()
            ctx.setupUser(leaderId)

            val event = PushEvent.InvitationResponse(
                recipientUserIds = listOf(leaderId),
                memberName = "Member",
                serviceName = "Service",
                scheduledDate = LocalDateTime.of(2025, 7, 20, 9, 0),
                accepted = true
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${leaderId.toString().take(8)}"), any()) }
        }
    }

    // =========================================================================
    // 16.6 — Payload de nueva canción (R7)
    // =========================================================================
    "16.6 NewSong payload" - {

        "payload contains title, artist and adder name" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.NewSong(
                recipientUserIds = listOf(recipientId),
                songTitle = "Oceans",
                artist = "Hillsong United",
                addedByName = "María García",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Oceans"
            payloadSlot.captured.body shouldContain "Hillsong United"
            payloadSlot.captured.body shouldContain "María García"
        }

        "payload handles null artist" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.NewSong(
                recipientUserIds = listOf(recipientId),
                songTitle = "Custom Song",
                artist = null,
                addedByName = "Juan",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Custom Song"
            payloadSlot.captured.body shouldNotContain "de null"
        }

        "recipients are active members except creator" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            // recipientUserIds already excludes creator (done by publishing service)
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.NewSong(
                recipientUserIds = listOf(member1, member2),
                songTitle = "Song",
                artist = "Artist",
                addedByName = "Creator",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.7 — Payload de invitación a iglesia (R8)
    // =========================================================================
    "16.7 ChurchInvitation payload" - {

        "payload contains church name and offered role" {
            val ctx = createContext()
            val invitedUser = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(invitedUser)

            val event = PushEvent.ChurchInvitation(
                recipientUserIds = listOf(invitedUser),
                churchName = "Iglesia Central",
                offeredRole = "Músico"
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Iglesia Central"
            payloadSlot.captured.body shouldContain "Músico"
        }

        "recipient is the invited user" {
            val ctx = createContext()
            val invitedUser = UUID.randomUUID()
            ctx.setupUser(invitedUser)

            val event = PushEvent.ChurchInvitation(
                recipientUserIds = listOf(invitedUser),
                churchName = "Church",
                offeredRole = "Member"
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${invitedUser.toString().take(8)}"), any()) }
        }
    }

    // =========================================================================
    // 16.8 — Payload de recordatorio de servicio (R9)
    // =========================================================================
    "16.8 ServiceReminder payload" - {

        "payload contains service name, hours until and setlist name" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.ServiceReminder(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio Dominical",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                setlistName = "Setlist Junio",
                hoursUntil = 2
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Servicio Dominical"
            payloadSlot.captured.body shouldContain "2h"
            payloadSlot.captured.body shouldContain "Setlist Junio"
        }

        "payload handles null setlist" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.ServiceReminder(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                setlistName = null,
                hoursUntil = 24
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "24h"
            payloadSlot.captured.body shouldNotContain "setlist"
        }

        "recipients are members with ACCEPTED status" {
            val ctx = createContext()
            val accepted1 = UUID.randomUUID()
            val accepted2 = UUID.randomUUID()
            // recipientUserIds already filtered to ACCEPTED members by publishing service
            ctx.setupUsers(listOf(accepted1, accepted2))

            val event = PushEvent.ServiceReminder(
                recipientUserIds = listOf(accepted1, accepted2),
                serviceName = "Service",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                setlistName = null,
                hoursUntil = 2
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.9 — Payload de modificación de setlist (R10)
    // =========================================================================
    "16.9 SetlistModified payload" - {

        "payload contains service name, date and change summary" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.SetlistModified(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio Dominical",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                changeSummary = "Se agregó 'Oceans' y se removió 'Way Maker'",
                serviceId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Servicio Dominical"
            payloadSlot.captured.body shouldContain "15/06/2025"
            payloadSlot.captured.body shouldContain "Se agregó 'Oceans' y se removió 'Way Maker'"
        }

        "recipients are assigned members" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.SetlistModified(
                recipientUserIds = listOf(member1, member2),
                serviceName = "Service",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                changeSummary = "Changes",
                serviceId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.10 — Payload de cancelación de servicio (R16)
    // =========================================================================
    "16.10 ServiceCancelled payload" - {

        "payload contains service name, original date and reason" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.ServiceCancelled(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio Nocturno",
                originalDate = LocalDateTime.of(2025, 6, 20, 19, 0),
                reason = "Mantenimiento del templo"
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Servicio Nocturno"
            payloadSlot.captured.body shouldContain "20/06/2025"
            payloadSlot.captured.body shouldContain "Mantenimiento del templo"
        }

        "payload handles null reason" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.ServiceCancelled(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio",
                originalDate = LocalDateTime.of(2025, 6, 20, 19, 0),
                reason = null
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "cancelado"
            payloadSlot.captured.body shouldNotContain "null"
        }

        "recipients are assigned members" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.ServiceCancelled(
                recipientUserIds = listOf(member1, member2),
                serviceName = "Service",
                originalDate = LocalDateTime.of(2025, 6, 20, 19, 0),
                reason = null
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }


    // =========================================================================
    // 16.11 — Payload de servicio recurrente creado (R17)
    // =========================================================================
    "16.11 RecurringServiceCreated payload" - {

        "payload contains service name, dates, recurrence pattern and role" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val dates = listOf(
                LocalDateTime.of(2025, 6, 15, 10, 0),
                LocalDateTime.of(2025, 6, 22, 10, 0),
                LocalDateTime.of(2025, 6, 29, 10, 0)
            )

            val event = PushEvent.RecurringServiceCreated(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio Semanal",
                scheduledDates = dates,
                recurrencePattern = "WEEKLY",
                roles = mapOf(memberId to "Guitarrista")
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Servicio Semanal"
            payloadSlot.captured.body shouldContain "semanal"
            payloadSlot.captured.body shouldContain "Guitarrista"
            payloadSlot.captured.body shouldContain "3 instancias"
        }

        "one consolidated notification per member, not one per instance" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            ctx.setupUser(memberId)

            val dates = listOf(
                LocalDateTime.of(2025, 6, 15, 10, 0),
                LocalDateTime.of(2025, 6, 22, 10, 0),
                LocalDateTime.of(2025, 6, 29, 10, 0),
                LocalDateTime.of(2025, 7, 6, 10, 0)
            )

            val event = PushEvent.RecurringServiceCreated(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio",
                scheduledDates = dates,
                recurrencePattern = "WEEKLY",
                roles = mapOf(memberId to "Miembro")
            )

            ctx.service.processPushEvent(event)

            // Only 1 push call per member, not 4
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "recipients are assigned members" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.RecurringServiceCreated(
                recipientUserIds = listOf(member1, member2),
                serviceName = "Service",
                scheduledDates = listOf(LocalDateTime.of(2025, 6, 15, 10, 0)),
                recurrencePattern = "MONTHLY",
                roles = mapOf(member1 to "Vocal", member2 to "Piano")
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.12 — Payload de actualización de regla de recurrencia (R18)
    // =========================================================================
    "16.12 RecurrenceRuleUpdated payload" - {

        "payload contains parent service name, new pattern and affected dates" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.RecurrenceRuleUpdated(
                recipientUserIds = listOf(memberId),
                parentServiceName = "Servicio Dominical",
                newRecurrencePattern = "BIWEEKLY",
                affectedDates = listOf(
                    LocalDateTime.of(2025, 7, 1, 10, 0),
                    LocalDateTime.of(2025, 7, 15, 10, 0)
                )
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Servicio Dominical"
            payloadSlot.captured.body shouldContain "quincenal"
            payloadSlot.captured.body shouldContain "2 instancias"
        }

        "payload includes removed dates when rule reduces instances" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.RecurrenceRuleUpdated(
                recipientUserIds = listOf(memberId),
                parentServiceName = "Servicio",
                newRecurrencePattern = "MONTHLY",
                affectedDates = listOf(LocalDateTime.of(2025, 7, 1, 10, 0)),
                removedDates = listOf(
                    LocalDateTime.of(2025, 7, 8, 10, 0),
                    LocalDateTime.of(2025, 7, 15, 10, 0)
                )
            )

            ctx.service.processPushEvent(event)

            // The payload should reflect the affected dates count
            payloadSlot.captured.body shouldContain "1 instancias"
        }

        "recipients are assigned members to affected future instances" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.RecurrenceRuleUpdated(
                recipientUserIds = listOf(member1, member2),
                parentServiceName = "Service",
                newRecurrencePattern = "WEEKLY",
                affectedDates = listOf(LocalDateTime.of(2025, 7, 1, 10, 0))
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.13 — Payload de eliminación de servicio recurrente (R19)
    // =========================================================================
    "16.13 RecurringServiceDeleted payload" - {

        "payload contains service name, affected dates and reason" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.RecurringServiceDeleted(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio Semanal",
                affectedDates = listOf(
                    LocalDateTime.of(2025, 7, 1, 10, 0),
                    LocalDateTime.of(2025, 7, 8, 10, 0),
                    LocalDateTime.of(2025, 7, 15, 10, 0)
                ),
                reason = "Reestructuración de horarios"
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Servicio Semanal"
            payloadSlot.captured.body shouldContain "3 instancias"
            payloadSlot.captured.body shouldContain "Reestructuración de horarios"
        }

        "payload handles null reason" {
            val ctx = createContext()
            val memberId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(memberId)

            val event = PushEvent.RecurringServiceDeleted(
                recipientUserIds = listOf(memberId),
                serviceName = "Servicio",
                affectedDates = listOf(LocalDateTime.of(2025, 7, 1, 10, 0)),
                reason = null
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "eliminado"
            payloadSlot.captured.body shouldNotContain "null"
        }

        "recipients are assigned members to deleted instances" {
            val ctx = createContext()
            val member1 = UUID.randomUUID()
            val member2 = UUID.randomUUID()
            ctx.setupUsers(listOf(member1, member2))

            val event = PushEvent.RecurringServiceDeleted(
                recipientUserIds = listOf(member1, member2),
                serviceName = "Service",
                affectedDates = listOf(LocalDateTime.of(2025, 7, 1, 10, 0)),
                reason = null
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.14 — Payload de actualización de canción (R20)
    // =========================================================================
    "16.14 SongUpdated payload" - {

        "payload contains title, changed fields and updater name" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.SongUpdated(
                recipientUserIds = listOf(recipientId),
                songTitle = "Oceans",
                changedFields = listOf("key", "bpm"),
                updatedByName = "María García",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Oceans"
            payloadSlot.captured.body shouldContain "key"
            payloadSlot.captured.body shouldContain "bpm"
            payloadSlot.captured.body shouldContain "María García"
        }

        "recipients are users with song in future setlists except updater" {
            val ctx = createContext()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            // recipientUserIds already filtered by publishing service: future setlists only, deduped, minus updater
            ctx.setupUsers(listOf(user1, user2))

            val event = PushEvent.SongUpdated(
                recipientUserIds = listOf(user1, user2),
                songTitle = "Song",
                changedFields = listOf("lyrics"),
                updatedByName = "Updater",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "only notifies users with future setlists - deduplication verified via single recipient list" {
            val ctx = createContext()
            val userId = UUID.randomUUID()
            ctx.setupUser(userId)

            // Even if user has song in multiple future setlists, they appear once in recipientUserIds
            val event = PushEvent.SongUpdated(
                recipientUserIds = listOf(userId), // already deduplicated
                songTitle = "Song",
                changedFields = listOf("chords"),
                updatedByName = "Updater",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            // Only 1 push sent, not multiple
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.15 — Payload de eliminación de canción (R21)
    // =========================================================================
    "16.15 SongDeleted payload" - {

        "payload contains title, deleter name and affected setlists" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.SongDeleted(
                recipientUserIds = listOf(recipientId),
                songTitle = "Way Maker",
                deletedByName = "Admin Juan",
                affectedSetlists = listOf("Setlist Junio", "Setlist Julio")
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Way Maker"
            payloadSlot.captured.body shouldContain "Admin Juan"
            payloadSlot.captured.body shouldContain "Setlist Junio"
            payloadSlot.captured.body shouldContain "Setlist Julio"
        }

        "payload handles empty affected setlists" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.SongDeleted(
                recipientUserIds = listOf(recipientId),
                songTitle = "Song",
                deletedByName = "Deleter",
                affectedSetlists = emptyList()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Song"
            payloadSlot.captured.body shouldContain "Deleter"
        }

        "recipients are users with song in setlists except deleter" {
            val ctx = createContext()
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            ctx.setupUsers(listOf(user1, user2))

            val event = PushEvent.SongDeleted(
                recipientUserIds = listOf(user1, user2),
                songTitle = "Song",
                deletedByName = "Deleter",
                affectedSetlists = listOf("Setlist A")
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }


    // =========================================================================
    // 16.16 — Payload de attachment agregado (R22)
    // =========================================================================
    "16.16 AttachmentAdded payload" - {

        "payload contains song title, attachment type and adder name" {
            val ctx = createContext()
            val recipientId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(recipientId)

            val event = PushEvent.AttachmentAdded(
                recipientUserIds = listOf(recipientId),
                songTitle = "Oceans",
                attachmentType = "YOUTUBE_LINK",
                addedByName = "Carlos López",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Oceans"
            payloadSlot.captured.body shouldContain "YOUTUBE_LINK"
            payloadSlot.captured.body shouldContain "Carlos López"
        }

        "recipients are creator plus previous commenters minus actor" {
            val ctx = createContext()
            val creator = UUID.randomUUID()
            val commenter = UUID.randomUUID()
            ctx.setupUsers(listOf(creator, commenter))

            val event = PushEvent.AttachmentAdded(
                recipientUserIds = listOf(creator, commenter),
                songTitle = "Song",
                attachmentType = "PDF_SHEET",
                addedByName = "Actor",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.17 — Payload de invitación aceptada (R23)
    // =========================================================================
    "16.17 InvitationAccepted payload" - {

        "payload contains new member name, email and accepted role" {
            val ctx = createContext()
            val adminId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(adminId)

            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(adminId),
                newMemberName = "Ana Martínez",
                newMemberEmail = "ana@example.com",
                acceptedRole = "Músico"
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Ana Martínez"
            payloadSlot.captured.body shouldContain "ana@example.com"
            payloadSlot.captured.body shouldContain "Músico"
        }

        "recipient is the admin who sent the invitation" {
            val ctx = createContext()
            val adminId = UUID.randomUUID()
            ctx.setupUser(adminId)

            val event = PushEvent.InvitationAccepted(
                recipientUserIds = listOf(adminId),
                newMemberName = "New Member",
                newMemberEmail = "new@test.com",
                acceptedRole = "Member"
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("fcm-token-${adminId.toString().take(8)}"), any()) }
        }
    }

    // =========================================================================
    // 16.18 — Payload de indisponibilidad (R24)
    // =========================================================================
    "16.18 MemberUnavailable payload" - {

        "payload contains member name, unavailable date and reason" {
            val ctx = createContext()
            val leaderId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(leaderId)

            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(leaderId),
                memberName = "Pedro Sánchez",
                unavailableDate = LocalDate.of(2025, 7, 10),
                reason = "Viaje familiar"
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Pedro Sánchez"
            payloadSlot.captured.body shouldContain "10/07/2025"
            payloadSlot.captured.body shouldContain "Viaje familiar"
        }

        "payload handles null reason" {
            val ctx = createContext()
            val leaderId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(leaderId)

            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(leaderId),
                memberName = "Pedro",
                unavailableDate = LocalDate.of(2025, 7, 10),
                reason = null
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Pedro"
            payloadSlot.captured.body shouldNotContain "null"
        }

        "recipients are team leaders" {
            val ctx = createContext()
            val leader1 = UUID.randomUUID()
            val leader2 = UUID.randomUUID()
            ctx.setupUsers(listOf(leader1, leader2))

            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(leader1, leader2),
                memberName = "Member",
                unavailableDate = LocalDate.of(2025, 7, 10),
                reason = null
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "no notification if member has no team - empty recipients" {
            val ctx = createContext()

            val event = PushEvent.MemberUnavailable(
                recipientUserIds = emptyList(),
                memberName = "Lonely Member",
                unavailableDate = LocalDate.of(2025, 7, 10),
                reason = null
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.19 — Payload de disponibilidad restaurada (R25)
    // =========================================================================
    "16.19 MemberAvailableAgain payload" - {

        "payload contains member name and previously unavailable date" {
            val ctx = createContext()
            val leaderId = UUID.randomUUID()
            val payloadSlot = ctx.setupUser(leaderId)

            val event = PushEvent.MemberAvailableAgain(
                recipientUserIds = listOf(leaderId),
                memberName = "Pedro Sánchez",
                previouslyUnavailableDate = LocalDate.of(2025, 7, 10)
            )

            ctx.service.processPushEvent(event)

            payloadSlot.captured.body shouldContain "Pedro Sánchez"
            payloadSlot.captured.body shouldContain "10/07/2025"
        }

        "recipients are team leaders" {
            val ctx = createContext()
            val leader1 = UUID.randomUUID()
            val leader2 = UUID.randomUUID()
            ctx.setupUsers(listOf(leader1, leader2))

            val event = PushEvent.MemberAvailableAgain(
                recipientUserIds = listOf(leader1, leader2),
                memberName = "Member",
                previouslyUnavailableDate = LocalDate.of(2025, 7, 10)
            )

            ctx.service.processPushEvent(event)

            verify(exactly = 2) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }

    // =========================================================================
    // 16.20 — Filtrado por preferencias (R11)
    // =========================================================================
    "16.20 Preference filtering" - {

        "push is omitted when preference is disabled but in-app is always stored" {
            val ctx = createContext()
            val userId = UUID.randomUUID()

            // User passes role filter (ADMIN)
            every { ctx.userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            // ChatMessage uses NotificationType.TEAM_ASSIGNMENT which maps to teamChanges preference
            every { ctx.prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(
                userId = userId,
                teamChanges = false
            )
            every { ctx.deviceTokenRepo.findByUserId(userId) } returns listOf(
                DeviceToken(userId = userId, token = "token-chat", platform = DevicePlatform.ANDROID)
            )

            val event = PushEvent.ChatMessage(
                recipientUserIds = listOf(userId),
                senderName = "Sender",
                teamName = "Team",
                messageExcerpt = "Hello",
                teamId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            // In-app notification IS stored
            verify(exactly = 1) {
                ctx.notifAppService.sendNotification(
                    userId = userId,
                    title = any(),
                    message = any(),
                    type = NotificationType.TEAM_ASSIGNMENT,
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
            // Push is NOT sent because teamChanges preference is disabled
            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "push is sent when preference is enabled" {
            val ctx = createContext()
            val userId = UUID.randomUUID()

            every { ctx.userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(
                userId = userId,
                chatMessages = true
            )
            every { ctx.deviceTokenRepo.findByUserId(userId) } returns listOf(
                DeviceToken(userId = userId, token = "token-chat-enabled", platform = DevicePlatform.ANDROID)
            )
            every { ctx.pushGateway.sendToDevices(any(), any()) } returns listOf(PushResult.Success("ok"))

            val event = PushEvent.ChatMessage(
                recipientUserIds = listOf(userId),
                senderName = "Sender",
                teamName = "Team",
                messageExcerpt = "Hello",
                teamId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            // Both in-app and push are sent
            verify(exactly = 1) {
                ctx.notifAppService.sendNotification(
                    userId = userId,
                    title = any(),
                    message = any(),
                    type = NotificationType.TEAM_ASSIGNMENT,
                    relatedEntityId = any(),
                    relatedEntityType = any()
                )
            }
            verify(exactly = 1) { ctx.pushGateway.sendToDevices(listOf("token-chat-enabled"), any()) }
        }

        "service assignment push omitted when serviceAssignments preference disabled" {
            val ctx = createContext()
            val userId = UUID.randomUUID()

            every { ctx.userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(
                userId = userId,
                serviceAssignments = false
            )

            val event = PushEvent.ServiceAssignment(
                recipientUserIds = listOf(userId),
                serviceName = "Service",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                roles = mapOf(userId to "Vocal")
            )

            ctx.service.processPushEvent(event)

            // In-app stored
            verify(exactly = 1) {
                ctx.notifAppService.sendNotification(
                    userId = userId, title = any(), message = any(),
                    type = NotificationType.SERVICE_INVITATION,
                    relatedEntityId = any(), relatedEntityType = any()
                )
            }
            // Push NOT sent
            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "song comment push omitted when songComments preference disabled" {
            val ctx = createContext()
            val userId = UUID.randomUUID()

            every { ctx.userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(
                userId = userId,
                songComments = false
            )

            val event = PushEvent.SongComment(
                recipientUserIds = listOf(userId),
                commenterName = "Commenter",
                songTitle = "Song",
                commentExcerpt = "Nice",
                songId = UUID.randomUUID()
            )

            ctx.service.processPushEvent(event)

            // In-app stored
            verify(exactly = 1) {
                ctx.notifAppService.sendNotification(
                    userId = userId, title = any(), message = any(),
                    type = NotificationType.NEW_COMMENT,
                    relatedEntityId = any(), relatedEntityType = any()
                )
            }
            // Push NOT sent
            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "availability change push omitted when availabilityChanges preference disabled" {
            val ctx = createContext()
            val userId = UUID.randomUUID()

            every { ctx.userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(
                userId = userId,
                availabilityChanges = false
            )

            val event = PushEvent.MemberUnavailable(
                recipientUserIds = listOf(userId),
                memberName = "Member",
                unavailableDate = LocalDate.of(2025, 7, 10),
                reason = null
            )

            ctx.service.processPushEvent(event)

            // In-app stored
            verify(exactly = 1) {
                ctx.notifAppService.sendNotification(
                    userId = userId, title = any(), message = any(),
                    type = NotificationType.AVAILABILITY_CHANGE,
                    relatedEntityId = any(), relatedEntityType = any()
                )
            }
            // Push NOT sent
            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }

        "recurring service push omitted when recurringServices preference disabled" {
            val ctx = createContext()
            val userId = UUID.randomUUID()

            every { ctx.userRoleResolver.resolveEffectiveRole(userId) } returns UserRole.ADMIN
            every { ctx.prefRepo.findByUserIdOrDefault(userId) } returns NotificationPreference(
                userId = userId,
                recurringServices = false
            )

            val event = PushEvent.RecurringServiceCreated(
                recipientUserIds = listOf(userId),
                serviceName = "Service",
                scheduledDates = listOf(LocalDateTime.of(2025, 6, 15, 10, 0)),
                recurrencePattern = "WEEKLY",
                roles = mapOf(userId to "Member")
            )

            ctx.service.processPushEvent(event)

            // In-app stored
            verify(exactly = 1) {
                ctx.notifAppService.sendNotification(
                    userId = userId, title = any(), message = any(),
                    type = NotificationType.RECURRING_SERVICE,
                    relatedEntityId = any(), relatedEntityType = any()
                )
            }
            // Push NOT sent
            verify(exactly = 0) { ctx.pushGateway.sendToDevices(any(), any()) }
        }
    }
})

/**
 * Helper context for PushNotificationPayloadTest tests.
 */
private data class TestCtx(
    val service: PushNotificationService,
    val pushGateway: PushGateway,
    val deviceTokenRepo: DeviceTokenRepository,
    val prefRepo: NotificationPreferenceRepository,
    val notifAppService: NotificationApplicationService,
    val userRoleResolver: UserRoleResolver
)
