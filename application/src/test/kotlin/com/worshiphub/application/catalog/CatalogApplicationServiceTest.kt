package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.repository.AttachmentRepository
import com.worshiphub.domain.catalog.repository.CategoryRepository
import com.worshiphub.domain.catalog.repository.GlobalSongRepository
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.domain.catalog.repository.TagRepository
import com.worshiphub.domain.collaboration.repository.SongCommentRepository
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogApplicationServiceTest {

    private val songRepository = mockk<SongRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val attachmentRepository = mockk<AttachmentRepository>()
    private val songCommentRepository = mockk<SongCommentRepository>()
    private val globalSongRepository = mockk<GlobalSongRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val serviceEventRepository = mockk<ServiceEventRepository>()
    private val setlistRepository = mockk<SetlistRepository>()
    private val catalogService = CatalogApplicationService(
        songRepository, categoryRepository, tagRepository, attachmentRepository,
        songCommentRepository, globalSongRepository, eventPublisher, userRepository,
        serviceEventRepository, setlistRepository
    )

    @Test
    fun `should create song with duplicate validation`() {
        val command = CreateSongCommand(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            bpm = 80,
            lyrics = null,
            chords = "[C]Amazing [F]Grace",
            churchId = UUID.randomUUID(),
            createdBy = UUID.randomUUID()
        )

        every { songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any()) } returns false
        every { songRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(any<UUID>()) } returns null
        every { userRepository.findByChurchIdAndIsActiveTrue(any()) } returns emptyList()

        val result = catalogService.createSong(command)

        verify { songRepository.save(any()) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should reject duplicate song creation`() {
        val command = CreateSongCommand(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            bpm = 80,
            lyrics = null,
            chords = "[C]Amazing [F]Grace",
            churchId = UUID.randomUUID(),
            createdBy = UUID.randomUUID()
        )

        every { songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any()) } returns true

        val result = catalogService.createSong(command)
        assertTrue(result.isFailure)
    }

    @Test
    fun `should search songs by title and artist`() {
        val churchId = UUID.randomUUID()
        val songs = listOf(
            Song(title = "Amazing Grace", artist = "John Newton", key = "C", churchId = churchId),
            Song(title = "How Great Thou Art", artist = "Carl Boberg", key = "G", churchId = churchId)
        )

        every { songRepository.searchByTitleOrArtist("grace", churchId, 0, 20) } returns songs.take(1)

        val result = catalogService.searchSongs("grace", churchId, 0, 20)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("Amazing Grace", result.getOrThrow()[0].title)
    }
}
