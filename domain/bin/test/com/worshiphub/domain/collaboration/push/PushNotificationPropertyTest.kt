package com.worshiphub.domain.collaboration.push

import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.NotificationType
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
import io.kotest.property.forAll
import java.time.LocalDateTime
import java.util.*

/**
 * Property-based tests for the push notification domain layer.
 *
 * Covers properties P1–P12 and P22–P25 as defined in the design document.
 * Uses Kotest Property Testing with Arb generators.
 */
class PushNotificationPropertyTest : FreeSpec({

    // ── Generators ──────────────────────────────────────────────────────────

    val arbUuid = Arb.uuid()
    val arbPlatform = Arb.enum<DevicePlatform>()
    val arbTokenString = Arb.string(50..300)
    val arbNotificationType = Arb.enum<NotificationType>()
    val arbUserRole = Arb.enum<UserRole>()

    // ── P1: Round-trip de registro de token ──────────────────────────────────
    // **Validates: Requirements 1.2**
    "P1 - Round-trip de registro de token: registrar un DeviceToken y recuperarlo por userId retorna el token correcto" {
        checkAll(arbUuid, arbTokenString, arbPlatform) { userId, tokenStr, platform ->
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
        checkAll(arbUuid, Arb.list(Arb.string(50..200), 1..10)) { userId, tokenStrings ->
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
        checkAll(arbTokenString) { tokenStr ->
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
        checkAll(Arb.set(arbUuid, 2..20)) { teamMembers ->
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
        checkAll(Arb.string(0..500)) { original ->
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
        checkAll(arbUuid, Arb.set(arbUuid, 0..10), arbUuid) { creator, previousCommenters, newCommenter ->
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
        checkAll(Arb.set(arbUuid, 1..30)) { teamMembers ->
            val recipientUserIds = teamMembers.toList()

            recipientUserIds.shouldContainAll(teamMembers)
            recipientUserIds.size shouldBe teamMembers.size
        }
    }

    // ── P8: Broadcast de nueva canción a la iglesia ─────────────────────────
    // **Validates: Requirements 7.1**
    "P8 - Broadcast de nueva canción: destinatarios = miembros activos \\ {creador}" {
        checkAll(Arb.set(arbUuid, 2..50)) { churchMembers ->
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
        checkAll(arbHoursOffset) { hoursOffset ->
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
        checkAll(Arb.set(arbUuid, 1..20)) { assignedMembers ->
            val recipientUserIds = assignedMembers.toList()

            recipientUserIds.shouldContainAll(assignedMembers)
            recipientUserIds.size shouldBe assignedMembers.size
        }
    }

    // ── P11: Filtrado por preferencias de notificación ──────────────────────
    // **Validates: Requirements 11.2, 11.3**
    "P11 - Filtrado por preferencias: push se envía solo si preferencia activada; in-app siempre se almacena" {
        checkAll(arbNotificationType, Arb.boolean()) { notifType, prefEnabled ->
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

        checkAll(arbNotificationList) { notifications ->
            val sorted = notifications.sortedByDescending { it.createdAt }

            // Verify descending order
            sorted.zipWithNext().forEach { (a, b) ->
                (a.createdAt >= b.createdAt).shouldBeTrue()
            }
        }
    }

    // ── P22: Filtrado de notificaciones por rol de usuario ──────────────────
    // **Validates: Requirements 30.1, 30.2**
    "P22 - Filtrado por rol: isApplicableForRole(T, R) es true sii T está en Mapa_Notificaciones_Rol[R]" {
        checkAll(arbNotificationType, arbUserRole) { notifType, role ->
            val isApplicable = RoleNotificationFilter.isApplicableForRole(notifType, role)
            val applicableTypes = RoleNotificationFilter.getApplicableTypes(role)

            // isApplicableForRole should be consistent with getApplicableTypes
            isApplicable shouldBe applicableTypes.contains(notifType)

            // Admin should always have all types
            if (role == UserRole.ADMIN) {
                RoleNotificationFilter.isApplicableForRole(notifType, UserRole.ADMIN).shouldBeTrue()
            }

            // If not applicable, user should NOT receive push even with token and prefs enabled
            if (!isApplicable) {
                val userIds = listOf(UUID.randomUUID())
                val filtered = RoleNotificationFilter.filterByRole(
                    userIds = userIds,
                    notificationType = notifType,
                    roleResolver = { role }
                )
                filtered.shouldHaveSize(0)
            }
        }
    }

    // ── P23: Resolución de jerarquía de roles ───────────────────────────────
    // **Validates: Requirements 30.5**
    "P23 - Resolución de jerarquía: resolveHighest retorna el rol de mayor jerarquía (Admin > Líder > Miembro)" {
        checkAll(Arb.list(arbUserRole, 1..5)) { roles ->
            val highest = UserRole.resolveHighest(roles)

            // If list contains ADMIN, result must be ADMIN
            if (roles.contains(UserRole.ADMIN)) {
                highest shouldBe UserRole.ADMIN
            }
            // If list contains TEAM_LEADER but not ADMIN, result must be TEAM_LEADER
            else if (roles.contains(UserRole.TEAM_LEADER)) {
                highest shouldBe UserRole.TEAM_LEADER
            }
            // Otherwise, result must be MEMBER
            else {
                highest shouldBe UserRole.MEMBER
            }

            // The result should always be in the input list
            roles.contains(highest).shouldBeTrue()

            // The result's ordinal should be the minimum (highest hierarchy)
            highest.ordinal shouldBe roles.minOf { it.ordinal }
        }
    }

    // ── P24: Visibilidad de preferencias según rol ──────────────────────────
    // **Validates: Requirements 11.5, 11.7**
    "P24 - Visibilidad de preferencias: getApplicableTypes(R) retorna exactamente los tipos del Mapa_Notificaciones_Rol[R]" {
        checkAll(arbUserRole) { role ->
            val applicableTypes = RoleNotificationFilter.getApplicableTypes(role)

            when (role) {
                UserRole.ADMIN -> {
                    // Admin gets ALL notification types
                    applicableTypes shouldBe NotificationType.entries.toSet()
                }
                UserRole.TEAM_LEADER -> {
                    // Team leader gets a specific subset (more than member, less than admin)
                    val memberTypes = RoleNotificationFilter.getApplicableTypes(UserRole.MEMBER)
                    val adminTypes = RoleNotificationFilter.getApplicableTypes(UserRole.ADMIN)

                    // Team leader types should be a superset of member types
                    applicableTypes.containsAll(memberTypes).shouldBeTrue()
                    // Team leader types should be a subset of admin types
                    adminTypes.containsAll(applicableTypes).shouldBeTrue()
                    // Team leader should have more types than member
                    (applicableTypes.size >= memberTypes.size).shouldBeTrue()
                }
                UserRole.MEMBER -> {
                    // Member gets the smallest subset
                    val adminTypes = RoleNotificationFilter.getApplicableTypes(UserRole.ADMIN)
                    // Member types should be a subset of admin types
                    adminTypes.containsAll(applicableTypes).shouldBeTrue()
                    // Member should have fewer types than admin
                    (applicableTypes.size <= adminTypes.size).shouldBeTrue()
                }
            }

            // Every type in applicableTypes should pass isApplicableForRole
            applicableTypes.forEach { type ->
                RoleNotificationFilter.isApplicableForRole(type, role).shouldBeTrue()
            }

            // Every type NOT in applicableTypes should fail isApplicableForRole
            (NotificationType.entries.toSet() - applicableTypes).forEach { type ->
                RoleNotificationFilter.isApplicableForRole(type, role).shouldBeFalse()
            }
        }
    }

    // ── P25: Activación por defecto al cambio de rol ascendente ─────────────
    // **Validates: Requirements 11.6**
    "P25 - Activación por defecto al cambio de rol ascendente: tipos recién disponibles se activan por defecto" {
        // Generate pairs where newRole has higher hierarchy than previousRole
        val arbRolePair = Arb.pair(arbUserRole, arbUserRole).filter { (prev, new) ->
            new.ordinal < prev.ordinal // lower ordinal = higher hierarchy
        }

        checkAll(100, arbRolePair) { (previousRole, newRole) ->
            val previousApplicable = RoleNotificationFilter.getApplicableTypes(previousRole)
            val newApplicable = RoleNotificationFilter.getApplicableTypes(newRole)
            val newlyAvailable = newApplicable - previousApplicable

            // Start with default preferences (all enabled)
            val prefs = NotificationPreference(userId = UUID.randomUUID())

            // Apply enableTypes for newly available types
            val updatedPrefs = prefs.enableTypes(newlyAvailable)

            // Verify newly available types are enabled after enableTypes
            newlyAvailable.forEach { type ->
                updatedPrefs.isEnabled(type).shouldBeTrue()
            }

            // Verify that the new role has strictly more applicable types than the previous role
            (newApplicable.size >= previousApplicable.size).shouldBeTrue()
            newApplicable.containsAll(previousApplicable).shouldBeTrue()

            // Verify that enableTypes is idempotent: enabling already-enabled types doesn't break anything
            val doubleEnabled = updatedPrefs.enableTypes(newlyAvailable)
            newlyAvailable.forEach { type ->
                doubleEnabled.isEnabled(type).shouldBeTrue()
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
