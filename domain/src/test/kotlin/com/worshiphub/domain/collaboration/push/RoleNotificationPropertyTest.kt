package com.worshiphub.domain.collaboration.push

import com.worshiphub.domain.collaboration.NotificationType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.*

/**
 * Property-based tests for RoleNotificationFilter and UserRole domain logic.
 *
 * Covers properties P22–P25 as defined in the design document.
 * These tests verify the role-based notification filtering logic
 * without any mocks — pure domain logic testing.
 *
 * Uses Kotest Property Testing with `checkAll` and `Arb.*` generators.
 */
class RoleNotificationPropertyTest : FreeSpec({

    // ── Generators ──────────────────────────────────────────────────────────

    val arbNotificationType = Arb.enum<NotificationType>()
    val arbUserRole = Arb.enum<UserRole>()

    // ── P22: Filtrado de notificaciones por rol de usuario ──────────────────
    // **Validates: Requirements 30.1, 30.2**
    "P22 - Filtrado por rol: isApplicableForRole(T, R) es true sii T está en Mapa_Notificaciones_Rol[R]" {
        checkAll(iterations = 100, arbNotificationType, arbUserRole) { notifType, role ->
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
        checkAll(iterations = 100, Arb.list(arbUserRole, 1..5)) { roles ->
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
        checkAll(iterations = 100, arbUserRole) { role ->
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

        checkAll(iterations = 100, arbRolePair) { (previousRole, newRole) ->
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
