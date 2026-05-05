package com.worshiphub.api.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

/**
 * Result of a church registration containing the church and admin user IDs.
 */
data class ChurchRegistrationResult(
    val churchId: UUID,
    val adminUserId: UUID
)

/**
 * Helper that creates prerequisite test data through the API using MockMvc.
 *
 * Every method authenticates its request via [TestSecurityHelper.withAuth] so that
 * the security context is correctly populated for controllers that read userId / churchId.
 */
class TestDataHelper(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    /**
     * Registers a new church with an admin user via POST /api/v1/auth/church/register.
     * This endpoint is public (permitAll), so no auth context is needed.
     */
    fun registerChurch(
        churchName: String = TestConstants.CHURCH_NAME,
        churchAddress: String = TestConstants.CHURCH_ADDRESS,
        churchEmail: String = TestConstants.CHURCH_EMAIL,
        adminEmail: String = TestConstants.ADMIN_EMAIL,
        adminFirstName: String = TestConstants.ADMIN_FIRST_NAME,
        adminLastName: String = TestConstants.ADMIN_LAST_NAME,
        adminPassword: String = TestConstants.VALID_PASSWORD
    ): ChurchRegistrationResult {
        val request = mapOf(
            "churchName" to churchName,
            "churchAddress" to churchAddress,
            "churchEmail" to churchEmail,
            "adminEmail" to adminEmail,
            "adminFirstName" to adminFirstName,
            "adminLastName" to adminLastName,
            "adminPassword" to adminPassword
        )

        val result = mockMvc.perform(
            post("/api/v1/auth/church/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val json = objectMapper.readTree(result.response.contentAsString)
        return ChurchRegistrationResult(
            churchId = UUID.fromString(json["churchId"].asText()),
            adminUserId = UUID.fromString(json["adminUserId"].asText())
        )
    }

    /**
     * Creates a team via POST /api/v1/teams.
     * Requires CHURCH_ADMIN or WORSHIP_LEADER role.
     */
    fun createTeam(
        churchId: UUID,
        leaderId: UUID,
        name: String = TestConstants.TEAM_NAME,
        description: String? = TestConstants.TEAM_DESCRIPTION
    ): UUID {
        val request = mapOf(
            "name" to name,
            "description" to description,
            "leaderId" to leaderId.toString()
        )

        val result = mockMvc.perform(
            post("/api/v1/teams")
                .with(TestSecurityHelper.withAuth(leaderId, churchId, listOf("CHURCH_ADMIN")))
                .header("Church-Id", churchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val json = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(json["teamId"].asText())
    }

    /**
     * Creates a song via POST /api/v1/songs.
     * Requires WORSHIP_LEADER or CHURCH_ADMIN role.
     */
    fun createSong(
        userId: UUID,
        churchId: UUID,
        title: String = TestConstants.SONG_TITLE,
        artist: String = TestConstants.SONG_ARTIST,
        key: String? = TestConstants.SONG_KEY,
        bpm: Int? = TestConstants.SONG_BPM,
        lyrics: String? = TestConstants.SONG_LYRICS,
        chords: String? = TestConstants.SONG_CHORDS
    ): UUID {
        val request = mutableMapOf<String, Any?>(
            "title" to title,
            "artist" to artist
        )
        key?.let { request["key"] = it }
        bpm?.let { request["bpm"] = it }
        lyrics?.let { request["lyrics"] = it }
        chords?.let { request["chords"] = it }

        val result = mockMvc.perform(
            post("/api/v1/songs")
                .with(TestSecurityHelper.withAuth(userId, churchId, listOf("CHURCH_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val json = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(json["id"].asText())
    }

    /**
     * Creates a setlist via POST /api/v1/setlists.
     * Requires WORSHIP_LEADER or CHURCH_ADMIN role and Church-Id header.
     */
    fun createSetlist(
        userId: UUID,
        churchId: UUID,
        name: String = TestConstants.SETLIST_NAME,
        songIds: List<UUID> = emptyList()
    ): UUID {
        val request = mapOf(
            "name" to name,
            "songIds" to songIds.map { it.toString() }
        )

        val result = mockMvc.perform(
            post("/api/v1/setlists")
                .with(TestSecurityHelper.withAuth(userId, churchId, listOf("CHURCH_ADMIN")))
                .header("Church-Id", churchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val json = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(json["setlistId"].asText())
    }

    /**
     * Creates a category via POST /api/v1/categories.
     * Requires WORSHIP_LEADER or CHURCH_ADMIN role.
     */
    fun createCategory(
        userId: UUID,
        churchId: UUID,
        name: String = TestConstants.CATEGORY_NAME,
        description: String? = TestConstants.CATEGORY_DESCRIPTION
    ): UUID {
        val request = mapOf(
            "name" to name,
            "description" to description
        )

        val result = mockMvc.perform(
            post("/api/v1/categories")
                .with(TestSecurityHelper.withAuth(userId, churchId, listOf("CHURCH_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val json = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(json["id"].asText())
    }

    /**
     * Creates a tag via POST /api/v1/tags.
     * Requires WORSHIP_LEADER or CHURCH_ADMIN role.
     */
    fun createTag(
        userId: UUID,
        churchId: UUID,
        name: String = TestConstants.TAG_NAME,
        color: String? = TestConstants.TAG_COLOR
    ): UUID {
        val request = mapOf(
            "name" to name,
            "color" to color
        )

        val result = mockMvc.perform(
            post("/api/v1/tags")
                .with(TestSecurityHelper.withAuth(userId, churchId, listOf("CHURCH_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val json = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(json["id"].asText())
    }
}
