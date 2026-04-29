package com.worshiphub.api.integration.catalog

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
import com.worshiphub.domain.catalog.GlobalSong
import com.worshiphub.domain.catalog.repository.GlobalSongRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

/**
 * E2E integration tests for the Catalog bounded context.
 * Covers Requirements 10-13: Songs CRUD, categories & tags, attachments & comments, global songs.
 */
class CatalogE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var globalSongRepository: GlobalSongRepository

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 10: Songs CRUD
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 10 - Songs CRUD")
    inner class SongsCrud {

        // Validates: Requirement 10.1
        @Test
        fun `should create song with complete data and return 201`() {
            val registration = testData.registerChurch(adminEmail = "song-create@testchurch.com")

            val request = mapOf(
                "title" to TestConstants.SONG_TITLE,
                "artist" to TestConstants.SONG_ARTIST,
                "key" to TestConstants.SONG_KEY,
                "bpm" to TestConstants.SONG_BPM,
                "lyrics" to TestConstants.SONG_LYRICS,
                "chords" to TestConstants.SONG_CHORDS
            )

            mockMvc.perform(
                post("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value(TestConstants.SONG_TITLE))
                .andExpect(jsonPath("$.artist").value(TestConstants.SONG_ARTIST))
                .andExpect(jsonPath("$.key").value(TestConstants.SONG_KEY))
                .andExpect(jsonPath("$.bpm").value(TestConstants.SONG_BPM))
                .andExpect(jsonPath("$.lyrics").value(TestConstants.SONG_LYRICS))
                .andExpect(jsonPath("$.chords").value(TestConstants.SONG_CHORDS))
                .andExpect(jsonPath("$.categories").isArray)
                .andExpect(jsonPath("$.tags").isArray)
                .andExpect(jsonPath("$.createdAt").exists())
        }

        // Validates: Requirement 10.2
        @Test
        fun `should list songs with pagination and return 200 with PageResponse`() {
            val registration = testData.registerChurch(adminEmail = "song-list@testchurch.com")

            // Create a song first
            testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                get("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .param("page", "0")
                    .param("size", "20")
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.hasNext").exists())
                .andExpect(jsonPath("$.hasPrevious").exists())
        }

        // Validates: Requirement 10.3
        @Test
        fun `should search songs by query and return 200 with matching songs`() {
            val registration = testData.registerChurch(adminEmail = "song-search@testchurch.com")

            // Create a song to search for
            testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "Searchable Song"
            )

            mockMvc.perform(
                get("/api/v1/songs/search")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .param("query", "Searchable")
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.hasNext").exists())
                .andExpect(jsonPath("$.hasPrevious").exists())
        }

        // Validates: Requirement 10.4
        @Test
        fun `should filter songs by categoryId and tagIds and return 200`() {
            val registration = testData.registerChurch(adminEmail = "song-filter@testchurch.com")

            // Create category and tag
            val categoryId = testData.createCategory(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )
            val tagId = testData.createTag(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            // Create a song
            testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                get("/api/v1/songs/filter")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .param("categoryId", categoryId.toString())
                    .param("tagIds", tagId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.hasNext").exists())
                .andExpect(jsonPath("$.hasPrevious").exists())
        }

        // Validates: Requirement 10.5
        @Test
        fun `should update song and return 200 with updated data`() {
            val registration = testData.registerChurch(adminEmail = "song-update@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val updateRequest = mapOf(
                "title" to "Updated Song Title",
                "artist" to "Updated Artist",
                "key" to "C",
                "bpm" to 100,
                "lyrics" to "Updated lyrics content",
                "chords" to "[C]Updated [G]chords"
            )

            mockMvc.perform(
                put("/api/v1/songs/$songId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.id").value(songId.toString()))
                .andExpect(jsonPath("$.title").value("Updated Song Title"))
                .andExpect(jsonPath("$.artist").value("Updated Artist"))
                .andExpect(jsonPath("$.key").value("C"))
                .andExpect(jsonPath("$.bpm").value(100))
        }

        // Validates: Requirement 10.6
        @Test
        fun `should delete song and return 204`() {
            val registration = testData.registerChurch(adminEmail = "song-delete@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                delete("/api/v1/songs/$songId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectNoContent()
        }

        // Validates: Business Rule - Song duplicate prevention
        @Test
        fun `should return 400 when creating song with duplicate title and artist in same church`() {
            val registration = testData.registerChurch(adminEmail = "song-dup@testchurch.com")

            // Create the first song
            testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "Duplicate Song",
                artist = "Duplicate Artist"
            )

            // Try to create another song with the same title and artist in the same church
            val duplicateRequest = mapOf(
                "title" to "Duplicate Song",
                "artist" to "Duplicate Artist",
                "key" to TestConstants.SONG_KEY,
                "bpm" to TestConstants.SONG_BPM
            )

            mockMvc.perform(
                post("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest))
            )
                .expectBadRequest()
        }

        // Validates: Requirement 10.7
        @Test
        fun `should return 400 when creating song with invalid data`() {
            val registration = testData.registerChurch(adminEmail = "song-invalid@testchurch.com")

            // Missing required title field
            val request = mapOf(
                "title" to "",
                "artist" to TestConstants.SONG_ARTIST
            )

            mockMvc.perform(
                post("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectBadRequest()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 11: Categories and Tags
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 11 - Categories and Tags")
    inner class CategoriesAndTags {

        // Validates: Requirement 11.1
        @Test
        fun `should create category and return 201 with id, name, description`() {
            val registration = testData.registerChurch(adminEmail = "cat-create@testchurch.com")

            val request = mapOf(
                "name" to TestConstants.CATEGORY_NAME,
                "description" to TestConstants.CATEGORY_DESCRIPTION
            )

            mockMvc.perform(
                post("/api/v1/categories")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(TestConstants.CATEGORY_NAME))
                .andExpect(jsonPath("$.description").value(TestConstants.CATEGORY_DESCRIPTION))
        }

        // Validates: Requirement 11.2
        @Test
        fun `should list categories and return 200`() {
            val registration = testData.registerChurch(adminEmail = "cat-list@testchurch.com")

            // Create a category first
            testData.createCategory(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                get("/api/v1/categories")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
        }

        // Validates: Requirement 11.3
        @Test
        fun `should update category and return 200 with updated data`() {
            val registration = testData.registerChurch(adminEmail = "cat-update@testchurch.com")

            val categoryId = testData.createCategory(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val updateRequest = mapOf(
                "name" to "Updated Category",
                "description" to "Updated description"
            )

            mockMvc.perform(
                put("/api/v1/categories/$categoryId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Category"))
                .andExpect(jsonPath("$.description").value("Updated description"))
        }

        // Validates: Requirement 11.4
        @Test
        fun `should delete category and return 204`() {
            val registration = testData.registerChurch(adminEmail = "cat-delete@testchurch.com")

            val categoryId = testData.createCategory(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                delete("/api/v1/categories/$categoryId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectNoContent()
        }

        // Validates: Requirement 11.5
        @Test
        fun `should create tag and return 201 with id, name, color`() {
            val registration = testData.registerChurch(adminEmail = "tag-create@testchurch.com")

            val request = mapOf(
                "name" to TestConstants.TAG_NAME,
                "color" to TestConstants.TAG_COLOR
            )

            mockMvc.perform(
                post("/api/v1/tags")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(TestConstants.TAG_NAME))
                .andExpect(jsonPath("$.color").value(TestConstants.TAG_COLOR))
        }

        // Validates: Requirement 11.6
        @Test
        fun `should list tags and return 200`() {
            val registration = testData.registerChurch(adminEmail = "tag-list@testchurch.com")

            // Create a tag first
            testData.createTag(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                get("/api/v1/tags")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
        }

        // Validates: Requirement 11.7
        @Test
        fun `should update tag and return 200 with updated data`() {
            val registration = testData.registerChurch(adminEmail = "tag-update@testchurch.com")

            val tagId = testData.createTag(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val updateRequest = mapOf(
                "name" to "Updated Tag",
                "color" to "#00FF00"
            )

            mockMvc.perform(
                put("/api/v1/tags/$tagId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.id").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Tag"))
                .andExpect(jsonPath("$.color").value("#00FF00"))
        }

        // Validates: Requirement 11.8
        @Test
        fun `should delete tag and return 204`() {
            val registration = testData.registerChurch(adminEmail = "tag-delete@testchurch.com")

            val tagId = testData.createTag(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                delete("/api/v1/tags/$tagId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectNoContent()
        }

        // Validates: Requirement 11.9
        @Test
        fun `should assign categories to song and return 200`() {
            val registration = testData.registerChurch(adminEmail = "cat-assign@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )
            val categoryId = testData.createCategory(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/categories")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(listOf(categoryId)))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Categories assigned successfully"))

            // Follow-up GET to verify the song actually has the category assigned
            mockMvc.perform(
                get("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$.content[0].categories").isArray)
                .andExpect(jsonPath("$.content[0].categories[0].id").value(categoryId.toString()))
        }

        // Validates: Requirement 11.10
        @Test
        fun `should assign tags to song and return 200`() {
            val registration = testData.registerChurch(adminEmail = "tag-assign@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )
            val tagId = testData.createTag(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/tags")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(listOf(tagId)))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Tags assigned successfully"))

            // Follow-up GET to verify the song actually has the tag assigned
            mockMvc.perform(
                get("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$.content[0].tags").isArray)
                .andExpect(jsonPath("$.content[0].tags[0].id").value(tagId.toString()))
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 12: Attachments and Comments
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 12 - Attachments and Comments")
    inner class AttachmentsAndComments {

        // Validates: Requirement 12.1
        @Test
        fun `should add attachment to song and return 201`() {
            val registration = testData.registerChurch(adminEmail = "attach-add@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val request = mapOf(
                "name" to "Lead Sheet PDF",
                "url" to "https://example.com/sheet.pdf",
                "type" to "PDF_SHEET"
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/attachments")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.attachmentId").exists())
                .andExpect(jsonPath("$.name").value("Lead Sheet PDF"))
                .andExpect(jsonPath("$.url").value("https://example.com/sheet.pdf"))
                .andExpect(jsonPath("$.type").exists())
        }

        // Validates: Requirement 12.2
        @Test
        fun `should return 400 when adding attachment with invalid data`() {
            val registration = testData.registerChurch(adminEmail = "attach-invalid@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            // Missing required fields and invalid URL
            val request = mapOf(
                "name" to "",
                "url" to "not-a-valid-url",
                "type" to "PDF_SHEET"
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/attachments")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectBadRequest()
        }

        // Validates: Requirement 12.3
        @Test
        fun `should add comment to song and return 201 with commentId`() {
            val registration = testData.registerChurch(adminEmail = "comment-add@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val request = mapOf(
                "content" to "Let's play this in D major for Sunday"
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/comments")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.commentId").exists())
        }

        // Validates: Requirement 12.4
        @Test
        fun `should list comments for song and return 200 with list`() {
            val registration = testData.registerChurch(adminEmail = "comment-list@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            // Add a comment first
            val commentRequest = mapOf(
                "content" to "Great arrangement!"
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/comments")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest))
            )
                .expectCreated()

            // List comments
            mockMvc.perform(
                get("/api/v1/songs/$songId/comments")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].content").value("Great arrangement!"))
                .andExpect(jsonPath("$[0].createdAt").exists())
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 13: Global Song Catalog
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 13 - Global Song Catalog")
    inner class GlobalSongCatalog {

        // Validates: Requirement 13.1
        @Test
        fun `should search global song catalog and return 200 with results`() {
            val registration = testData.registerChurch(adminEmail = "global-search@testchurch.com")

            // Create global songs in the DB first
            globalSongRepository.save(
                GlobalSong(
                    title = "Amazing Grace",
                    artist = "John Newton",
                    key = "G",
                    bpm = 80,
                    isVerified = true
                )
            )
            globalSongRepository.save(
                GlobalSong(
                    title = "How Great Is Our God",
                    artist = "Chris Tomlin",
                    key = "C",
                    bpm = 78,
                    isVerified = true
                )
            )

            mockMvc.perform(
                get("/api/v1/global-songs/search")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .param("query", "Amazing Grace")
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").value("Amazing Grace"))
                .andExpect(jsonPath("$[0].artist").value("John Newton"))
                .andExpect(jsonPath("$[0].key").value("G"))
                .andExpect(jsonPath("$[0].isVerified").value(true))
        }

        // Validates: Requirement 13.2
        @Test
        fun `should import global song and return 201 with songId`() {
            val registration = testData.registerChurch(adminEmail = "global-import@testchurch.com")

            // Create a global song to import
            val globalSong = globalSongRepository.save(
                GlobalSong(
                    title = "10,000 Reasons",
                    artist = "Matt Redman",
                    key = "G",
                    bpm = 73,
                    chords = "[G]Bless the [C]Lord",
                    isVerified = true
                )
            )

            mockMvc.perform(
                post("/api/v1/global-songs/${globalSong.id}/import")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectCreated()
                .andExpect(jsonPath("$.songId").exists())
        }
    }
}
