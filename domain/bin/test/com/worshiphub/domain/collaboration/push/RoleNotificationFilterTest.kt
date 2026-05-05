package com.worshiphub.domain.collaboration.push

import com.worshiphub.domain.collaboration.NotificationType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.util.*

/**
 * Unit tests for RoleNotificationFilter.
 * Validates: Requirements 30.1, 30.2, 30.3, 29.1
 */
class RoleNotificationFilterTest : FreeSpec({

    "isApplicableForRole" - {

        "returns true for Admin with any notification type" {
            NotificationType.entries.forEach { type ->
                RoleNotificationFilter.isApplicableForRole(type, UserRole.ADMIN) shouldBe true
            }
        }

        "returns false for Member with INVITATION_ACCEPTED" {
            RoleNotificationFilter.isApplicableForRole(
                NotificationType.INVITATION_ACCEPTED, UserRole.MEMBER
            ) shouldBe false
        }

        "returns false for Member with AVAILABILITY_CHANGE" {
            RoleNotificationFilter.isApplicableForRole(
                NotificationType.AVAILABILITY_CHANGE, UserRole.MEMBER
            ) shouldBe false
        }

        "returns true for TeamLeader with AVAILABILITY_CHANGE" {
            RoleNotificationFilter.isApplicableForRole(
                NotificationType.AVAILABILITY_CHANGE, UserRole.TEAM_LEADER
            ) shouldBe true
        }
    }

    "getApplicableTypes" - {

        "ADMIN contains all NotificationType values" {
            val adminTypes = RoleNotificationFilter.getApplicableTypes(UserRole.ADMIN)
            adminTypes shouldBe NotificationType.entries.toSet()
        }

        "TEAM_LEADER contains the correct subset" {
            val leaderTypes = RoleNotificationFilter.getApplicableTypes(UserRole.TEAM_LEADER)
            leaderTypes shouldContainAll listOf(
                NotificationType.SERVICE_INVITATION,
                NotificationType.CHAT_MESSAGE,
                NotificationType.NEW_COMMENT,
                NotificationType.TEAM_MEMBER_ADDED,
                NotificationType.TEAM_MEMBER_REMOVED,
                NotificationType.TEAM_LEADER_CHANGED,
                NotificationType.TEAM_ROLE_CHANGED,
                NotificationType.TEAM_ASSIGNMENT,
                NotificationType.NEW_SONG,
                NotificationType.SERVICE_SCHEDULED,
                NotificationType.RECURRING_SERVICE,
                NotificationType.SONG_UPDATED,
                NotificationType.SONG_DELETED,
                NotificationType.SONG_ATTACHMENT,
                NotificationType.AVAILABILITY_CHANGE
            )
            leaderTypes shouldNotContain NotificationType.INVITATION_ACCEPTED
        }

        "MEMBER contains the correct subset" {
            val memberTypes = RoleNotificationFilter.getApplicableTypes(UserRole.MEMBER)
            memberTypes shouldContainAll listOf(
                NotificationType.SERVICE_INVITATION,
                NotificationType.CHAT_MESSAGE,
                NotificationType.NEW_COMMENT,
                NotificationType.NEW_SONG,
                NotificationType.SERVICE_SCHEDULED,
                NotificationType.RECURRING_SERVICE,
                NotificationType.SONG_UPDATED,
                NotificationType.SONG_DELETED,
                NotificationType.SONG_ATTACHMENT
            )
            memberTypes shouldNotContain NotificationType.INVITATION_ACCEPTED
            memberTypes shouldNotContain NotificationType.AVAILABILITY_CHANGE
            memberTypes shouldNotContain NotificationType.TEAM_MEMBER_ADDED
        }
    }

    "filterByRole" - {

        "removes users whose role does not allow the notification type" {
            val adminId = UUID.randomUUID()
            val memberId = UUID.randomUUID()
            val userIds = listOf(adminId, memberId)

            val result = RoleNotificationFilter.filterByRole(
                userIds = userIds,
                notificationType = NotificationType.INVITATION_ACCEPTED,
                roleResolver = { userId ->
                    if (userId == adminId) UserRole.ADMIN else UserRole.MEMBER
                }
            )

            result shouldBe listOf(adminId)
        }

        "keeps users whose role allows the notification type" {
            val adminId = UUID.randomUUID()
            val memberId = UUID.randomUUID()
            val userIds = listOf(adminId, memberId)

            val result = RoleNotificationFilter.filterByRole(
                userIds = userIds,
                notificationType = NotificationType.SERVICE_INVITATION,
                roleResolver = { UserRole.MEMBER }
            )

            result shouldBe listOf(adminId, memberId)
        }
    }
})
