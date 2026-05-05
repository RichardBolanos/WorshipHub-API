package com.worshiphub.domain.collaboration.push

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for UserRole enum and hierarchy resolution.
 * Validates: Requirements 30.5, 29.1
 */
class UserRoleTest : FreeSpec({

    "resolveHighest" - {

        "resolveHighest([ADMIN, MEMBER]) returns ADMIN" {
            UserRole.resolveHighest(listOf(UserRole.ADMIN, UserRole.MEMBER)) shouldBe UserRole.ADMIN
        }

        "resolveHighest([TEAM_LEADER, MEMBER]) returns TEAM_LEADER" {
            UserRole.resolveHighest(listOf(UserRole.TEAM_LEADER, UserRole.MEMBER)) shouldBe UserRole.TEAM_LEADER
        }

        "resolveHighest([MEMBER]) returns MEMBER" {
            UserRole.resolveHighest(listOf(UserRole.MEMBER)) shouldBe UserRole.MEMBER
        }

        "resolveHighest([ADMIN, TEAM_LEADER, MEMBER]) returns ADMIN" {
            UserRole.resolveHighest(
                listOf(UserRole.ADMIN, UserRole.TEAM_LEADER, UserRole.MEMBER)
            ) shouldBe UserRole.ADMIN
        }

        "resolveHighest(emptyList()) returns MEMBER as fallback" {
            UserRole.resolveHighest(emptyList()) shouldBe UserRole.MEMBER
        }
    }

    "enum ordering" - {

        "values are ordered by hierarchy - ADMIN highest" {
            (UserRole.ADMIN.ordinal < UserRole.TEAM_LEADER.ordinal) shouldBe true
            (UserRole.TEAM_LEADER.ordinal < UserRole.MEMBER.ordinal) shouldBe true
        }

        "enum has exactly three values" {
            UserRole.entries.size shouldBe 3
        }
    }
})
