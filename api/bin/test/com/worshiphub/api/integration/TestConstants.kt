package com.worshiphub.api.integration

/**
 * Shared constants for E2E integration tests.
 */
object TestConstants {
    // Auth constants
    const val VALID_PASSWORD = "SecurePass123!"
    const val WEAK_PASSWORD = "123"
    const val VALID_EMAIL = "test@worshiphub.com"
    const val ADMIN_EMAIL = "admin@testchurch.com"
    const val ADMIN_FIRST_NAME = "Admin"
    const val ADMIN_LAST_NAME = "User"

    // Church constants
    const val CHURCH_NAME = "Iglesia de Prueba"
    const val CHURCH_ADDRESS = "Calle Test 123"
    const val CHURCH_EMAIL = "church@test.com"

    // Team constants
    const val TEAM_NAME = "Worship Team"
    const val TEAM_DESCRIPTION = "Main worship team for Sunday services"

    // Song constants
    const val SONG_TITLE = "Amazing Grace"
    const val SONG_ARTIST = "John Newton"
    const val SONG_KEY = "G"
    const val SONG_BPM = 120
    const val SONG_LYRICS = "Amazing grace, how sweet the sound"
    const val SONG_CHORDS = "[G]Amazing [C]grace"

    // Category & Tag constants
    const val CATEGORY_NAME = "Worship"
    const val CATEGORY_DESCRIPTION = "Songs for worship time"
    const val TAG_NAME = "Contemporary"
    const val TAG_COLOR = "#FF5733"

    // Setlist constants
    const val SETLIST_NAME = "Sunday Setlist"

    // Roles
    const val ROLE_CHURCH_ADMIN = "CHURCH_ADMIN"
    const val ROLE_WORSHIP_LEADER = "WORSHIP_LEADER"
    const val ROLE_TEAM_MEMBER = "TEAM_MEMBER"
    const val ROLE_SUPER_ADMIN = "SUPER_ADMIN"
}
