package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.Category
import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.Tag
import com.worshiphub.domain.catalog.repository.AttachmentRepository
import com.worshiphub.domain.catalog.repository.CategoryRepository
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.domain.catalog.repository.TagRepository
import com.worshiphub.domain.collaboration.repository.SongCommentRepository
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.*

/**
 * Feature: song-tags-categories
 *
 * Property 1: Round-trip de creación de canción con asociaciones
 * For any valid song and any subset of existing tag/category IDs, creating the song
 * with those IDs and then retrieving it should return the exact same tag and category associations.
 * Validates: Requirements 1.3, 3.3, 8.1, 8.3
 *
 * Property 2: Actualización reemplaza asociaciones completamente
 * For any existing song with tag/category associations, updating with a new set of IDs
 * should completely replace the old associations with the new ones.
 * Validates: Requirements 2.3, 8.2, 8.4
 *
 * Property 3: IDs inválidos producen error 400
 * For any create or update request containing at least one tag or category ID that does not
 * exist in the database, the service should return a Result.failure with an IllegalArgumentException.
 * Validates: Requirements 1.5
 *
 * Property 4: IDs nulos preservan asociaciones existentes en actualización
 * For any existing song with tag/category associations, updating the song without including
 * tagIds or categoryIds (null fields) should preserve the existing associations unchanged.
 * Validates: Requirements 8.5
 */
@OptIn(ExperimentalKotest::class)
class SongAssociationsPropertyTest : FunSpec({

    val churchId = UUID.randomUUID()

    // Pre-create a pool of "existing" tags and categories to pick subsets from
    val existingTags = (1..10).map { i ->
        Tag(
            id = UUID.randomUUID(),
            name = "Tag$i",
            color = "#${"%06x".format(i * 111111 % 0xFFFFFF)}",
            churchId = churchId
        )
    }

    val existingCategories = (1..10).map { i ->
        Category(
            id = UUID.randomUUID(),
            name = "Category$i",
            description = "Description $i",
            churchId = churchId
        )
    }

    // Custom generator: random subset of a list
    fun <T> Arb.Companion.subset(items: List<T>): Arb<List<T>> = arbitrary {
        val bitmask = it.random.nextInt(0, 1 shl items.size)
        items.filterIndexed { index, _ -> bitmask and (1 shl index) != 0 }
    }

    // Generators
    val titleArb = Arb.string(minSize = 1, maxSize = 50, codepoints = Codepoint.alphanumeric())
    val artistArb = Arb.string(minSize = 1, maxSize = 30, codepoints = Codepoint.alphanumeric()).orNull(0.2)
    val keyArb = Arb.element("C", "D", "E", "F", "G", "A", "B", "Am", "Dm", "Em").orNull(0.3)
    val bpmArb = Arb.int(60..200).orNull(0.3)
    val tagSubsetArb = Arb.subset(existingTags)
    val categorySubsetArb = Arb.subset(existingCategories)

    test("Property 1: Round-trip de creación de canción con asociaciones") {
        checkAll(
            PropTestConfig(iterations = 100),
            titleArb,
            artistArb,
            keyArb,
            bpmArb,
            tagSubsetArb,
            categorySubsetArb
        ) { title, artist, key, bpm, selectedTags, selectedCategories ->

            // Setup mocks fresh for each iteration
            val songRepository = mockk<SongRepository>()
            val tagRepository = mockk<TagRepository>()
            val categoryRepository = mockk<CategoryRepository>()
            val attachmentRepository = mockk<AttachmentRepository>()
            val songCommentRepository = mockk<SongCommentRepository>()

            val service = CatalogApplicationService(
                songRepository,
                categoryRepository,
                tagRepository,
                attachmentRepository,
                songCommentRepository
            )

            // No duplicates
            every { songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any()) } returns false

            // Tag lookups
            existingTags.forEach { tag ->
                every { tagRepository.findById(tag.id) } returns tag
            }

            // Category lookups
            existingCategories.forEach { cat ->
                every { categoryRepository.findById(cat.id) } returns cat
            }

            // Capture saved song and return it on findById
            val savedSongSlot = slot<Song>()
            every { songRepository.save(capture(savedSongSlot)) } answers { savedSongSlot.captured }
            every { songRepository.findById(any()) } answers {
                if (savedSongSlot.isCaptured) savedSongSlot.captured else null
            }

            val tagIds = selectedTags.map { it.id }
            val categoryIds = selectedCategories.map { it.id }

            val command = CreateSongCommand(
                title = title,
                artist = artist,
                key = key,
                bpm = bpm,
                lyrics = null,
                chords = null,
                churchId = churchId,
                createdBy = UUID.randomUUID(),
                tagIds = tagIds.ifEmpty { null },
                categoryIds = categoryIds.ifEmpty { null }
            )

            // Create the song
            val createResult = service.createSong(command)
            createResult.shouldBeSuccess()
            val createdSong = createResult.getOrThrow()

            // Retrieve the song
            val getResult = service.getSongById(createdSong.id)
            getResult.shouldBeSuccess()
            val retrievedSong = getResult.getOrThrow()

            // Verify associations match
            retrievedSong.tags.map { it.id }.toSet() shouldBe selectedTags.map { it.id }.toSet()
            retrievedSong.categories.map { it.id }.toSet() shouldBe selectedCategories.map { it.id }.toSet()

            // Verify song fields
            retrievedSong.title shouldBe title
            retrievedSong.artist shouldBe artist
            retrievedSong.key shouldBe key
            retrievedSong.bpm shouldBe bpm
        }
    }

    // Feature: song-tags-categories, Property 2: Actualización reemplaza asociaciones completamente
    test("Property 2: Actualización reemplaza asociaciones completamente") {
        checkAll(
            PropTestConfig(iterations = 100),
            titleArb,
            artistArb,
            keyArb,
            bpmArb,
            tagSubsetArb,
            categorySubsetArb,
            tagSubsetArb,
            categorySubsetArb
        ) { title, artist, key, bpm, initialTags, initialCategories, newTags, newCategories ->

            // Setup mocks fresh for each iteration
            val songRepository = mockk<SongRepository>()
            val tagRepository = mockk<TagRepository>()
            val categoryRepository = mockk<CategoryRepository>()
            val attachmentRepository = mockk<AttachmentRepository>()
            val songCommentRepository = mockk<SongCommentRepository>()

            val service = CatalogApplicationService(
                songRepository,
                categoryRepository,
                tagRepository,
                attachmentRepository,
                songCommentRepository
            )

            // No duplicates
            every { songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any()) } returns false

            // Tag lookups
            existingTags.forEach { tag ->
                every { tagRepository.findById(tag.id) } returns tag
            }

            // Category lookups
            existingCategories.forEach { cat ->
                every { categoryRepository.findById(cat.id) } returns cat
            }

            // Track the latest saved song so findById always returns the most recent version
            var latestSavedSong: Song? = null
            every { songRepository.save(any()) } answers {
                val song = firstArg<Song>()
                latestSavedSong = song
                song
            }
            every { songRepository.findById(any()) } answers {
                latestSavedSong
            }

            // Step 1: Create song with initial associations
            val createCommand = CreateSongCommand(
                title = title,
                artist = artist,
                key = key,
                bpm = bpm,
                lyrics = null,
                chords = null,
                churchId = churchId,
                createdBy = UUID.randomUUID(),
                tagIds = initialTags.map { it.id }.ifEmpty { null },
                categoryIds = initialCategories.map { it.id }.ifEmpty { null }
            )

            val createResult = service.createSong(createCommand)
            createResult.shouldBeSuccess()
            val createdSong = createResult.getOrThrow()

            // Step 2: Update with new set of associations
            val updateCommand = UpdateSongCommand(
                title = title,
                artist = artist,
                key = key,
                bpm = bpm,
                tagIds = newTags.map { it.id },
                categoryIds = newCategories.map { it.id }
            )

            val updateResult = service.updateSong(createdSong.id, updateCommand)
            updateResult.shouldBeSuccess()

            // Step 3: Retrieve and verify associations are exactly the new set
            val getResult = service.getSongById(createdSong.id)
            getResult.shouldBeSuccess()
            val retrievedSong = getResult.getOrThrow()

            // Verify: associations must be exactly the new set, no old ones retained
            retrievedSong.tags.map { it.id }.toSet() shouldBe newTags.map { it.id }.toSet()
            retrievedSong.categories.map { it.id }.toSet() shouldBe newCategories.map { it.id }.toSet()
        }
    }

    // Feature: song-tags-categories, Property 3: IDs inválidos producen error 400
    test("Property 3: IDs inválidos producen error 400") {
        // Validates: Requirements 1.5

        // Generator for lists of 1..5 random UUIDs that won't exist in any repository
        val invalidUuidListArb = Arb.list(Arb.uuid(), range = 1..5)

        checkAll(
            PropTestConfig(iterations = 100),
            titleArb,
            artistArb,
            invalidUuidListArb,
            invalidUuidListArb,
            Arb.boolean()
        ) { title, artist, invalidTagIds, invalidCategoryIds, testCreate ->

            // Setup mocks fresh for each iteration
            val songRepository = mockk<SongRepository>()
            val tagRepository = mockk<TagRepository>()
            val categoryRepository = mockk<CategoryRepository>()
            val attachmentRepository = mockk<AttachmentRepository>()
            val songCommentRepository = mockk<SongCommentRepository>()

            val service = CatalogApplicationService(
                songRepository,
                categoryRepository,
                tagRepository,
                attachmentRepository,
                songCommentRepository
            )

            // All random UUIDs return null (not found)
            every { tagRepository.findById(any()) } returns null
            every { categoryRepository.findById(any()) } returns null

            if (testCreate) {
                // --- Test CREATE with invalid tag IDs ---
                every { songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any()) } returns false

                val createWithInvalidTags = CreateSongCommand(
                    title = title,
                    artist = artist,
                    key = null,
                    bpm = null,
                    lyrics = null,
                    chords = null,
                    churchId = churchId,
                    createdBy = UUID.randomUUID(),
                    tagIds = invalidTagIds,
                    categoryIds = null
                )

                val resultTags = service.createSong(createWithInvalidTags)
                resultTags.shouldBeFailure()
                resultTags.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
                resultTags.exceptionOrNull()!!.message shouldContain "Invalid tag IDs"

                // --- Test CREATE with invalid category IDs ---
                val createWithInvalidCategories = CreateSongCommand(
                    title = title,
                    artist = artist,
                    key = null,
                    bpm = null,
                    lyrics = null,
                    chords = null,
                    churchId = churchId,
                    createdBy = UUID.randomUUID(),
                    tagIds = null,
                    categoryIds = invalidCategoryIds
                )

                val resultCategories = service.createSong(createWithInvalidCategories)
                resultCategories.shouldBeFailure()
                resultCategories.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
                resultCategories.exceptionOrNull()!!.message shouldContain "Invalid category IDs"
            } else {
                // --- Test UPDATE with invalid tag/category IDs ---
                // Create a valid existing song for the update scenario
                val existingSong = Song(
                    title = title,
                    artist = artist,
                    key = null,
                    churchId = churchId
                )
                every { songRepository.findById(existingSong.id) } returns existingSong

                val updateWithInvalidTags = UpdateSongCommand(
                    title = title,
                    artist = artist,
                    tagIds = invalidTagIds,
                    categoryIds = null
                )

                val updateResultTags = service.updateSong(existingSong.id, updateWithInvalidTags)
                updateResultTags.shouldBeFailure()
                updateResultTags.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
                updateResultTags.exceptionOrNull()!!.message shouldContain "Invalid tag IDs"

                val updateWithInvalidCategories = UpdateSongCommand(
                    title = title,
                    artist = artist,
                    tagIds = null,
                    categoryIds = invalidCategoryIds
                )

                val updateResultCategories = service.updateSong(existingSong.id, updateWithInvalidCategories)
                updateResultCategories.shouldBeFailure()
                updateResultCategories.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
                updateResultCategories.exceptionOrNull()!!.message shouldContain "Invalid category IDs"
            }
        }
    }

    // Feature: song-tags-categories, Property 4: IDs nulos preservan asociaciones existentes en actualización
    test("Property 4: IDs nulos preservan asociaciones existentes en actualización") {
        // Validates: Requirements 8.5
        checkAll(
            PropTestConfig(iterations = 100),
            titleArb,
            artistArb,
            keyArb,
            bpmArb,
            tagSubsetArb,
            categorySubsetArb
        ) { title, artist, key, bpm, initialTags, initialCategories ->

            // Setup mocks fresh for each iteration
            val songRepository = mockk<SongRepository>()
            val tagRepository = mockk<TagRepository>()
            val categoryRepository = mockk<CategoryRepository>()
            val attachmentRepository = mockk<AttachmentRepository>()
            val songCommentRepository = mockk<SongCommentRepository>()

            val service = CatalogApplicationService(
                songRepository,
                categoryRepository,
                tagRepository,
                attachmentRepository,
                songCommentRepository
            )

            // No duplicates
            every { songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any()) } returns false

            // Tag lookups
            existingTags.forEach { tag ->
                every { tagRepository.findById(tag.id) } returns tag
            }

            // Category lookups
            existingCategories.forEach { cat ->
                every { categoryRepository.findById(cat.id) } returns cat
            }

            // Track the latest saved song so findById always returns the most recent version
            var latestSavedSong: Song? = null
            every { songRepository.save(any()) } answers {
                val song = firstArg<Song>()
                latestSavedSong = song
                song
            }
            every { songRepository.findById(any()) } answers {
                latestSavedSong
            }

            // Step 1: Create song with initial associations
            val createCommand = CreateSongCommand(
                title = title,
                artist = artist,
                key = key,
                bpm = bpm,
                lyrics = null,
                chords = null,
                churchId = churchId,
                createdBy = UUID.randomUUID(),
                tagIds = initialTags.map { it.id }.ifEmpty { null },
                categoryIds = initialCategories.map { it.id }.ifEmpty { null }
            )

            val createResult = service.createSong(createCommand)
            createResult.shouldBeSuccess()
            val createdSong = createResult.getOrThrow()

            // Capture the original associations before update
            val originalTagIds = createdSong.tags.map { it.id }.toSet()
            val originalCategoryIds = createdSong.categories.map { it.id }.toSet()

            // Step 2: Update with tagIds=null and categoryIds=null (should preserve existing)
            val updateCommand = UpdateSongCommand(
                title = title,
                artist = artist,
                key = key,
                bpm = bpm,
                tagIds = null,
                categoryIds = null
            )

            val updateResult = service.updateSong(createdSong.id, updateCommand)
            updateResult.shouldBeSuccess()

            // Step 3: Retrieve and verify original associations are still intact
            val getResult = service.getSongById(createdSong.id)
            getResult.shouldBeSuccess()
            val retrievedSong = getResult.getOrThrow()

            // Verify: associations must be exactly the original set, unchanged
            retrievedSong.tags.map { it.id }.toSet() shouldBe originalTagIds
            retrievedSong.categories.map { it.id }.toSet() shouldBe originalCategoryIds
        }
    }

    // Feature: song-tags-categories, Property 5: Filtrado por categoría y etiquetas
    test("Property 5: Filtrado por categoría y etiquetas") {
        // Validates: Requirements 6.1, 6.2, 6.3

        // Generator for a list of songs with varied tag/category associations
        val songCountArb = Arb.int(1..15)
        val filterCategoryArb = Arb.element(existingCategories.map { it.id } + listOf(null))
        val filterTagSubsetArb = Arb.subset(existingTags.map { it.id })

        checkAll(
            PropTestConfig(iterations = 100),
            songCountArb,
            filterCategoryArb,
            filterTagSubsetArb
        ) { songCount, filterCategoryId, filterTagIds ->

            // Generate a pool of songs with random tag/category associations
            val allSongs = (1..songCount).map { i ->
                val songTags = existingTags.filterIndexed { idx, _ ->
                    (i + idx) % 3 == 0 || (i * idx) % 5 == 0
                }.toSet()
                val songCategories = existingCategories.filterIndexed { idx, _ ->
                    (i + idx) % 4 == 0 || (i * idx) % 7 == 0
                }.toSet()
                Song(
                    title = "Song$i",
                    artist = "Artist$i",
                    key = "C",
                    churchId = churchId,
                    tags = songTags,
                    categories = songCategories
                )
            }

            // Compute expected results using the filtering contract:
            // - If categoryId is specified, song must belong to that category
            // - If tagIds is non-empty, song must have at least one of the selected tags
            // - Both criteria must be satisfied simultaneously (AND)
            val expectedSongs = allSongs.filter { song ->
                val matchesCategory = filterCategoryId == null ||
                    song.categories.any { it.id == filterCategoryId }
                val matchesTags = filterTagIds.isEmpty() ||
                    song.tags.any { it.id in filterTagIds }
                matchesCategory && matchesTags
            }

            // Setup mocks
            val songRepository = mockk<SongRepository>()
            val tagRepository = mockk<TagRepository>()
            val categoryRepository = mockk<CategoryRepository>()
            val attachmentRepository = mockk<AttachmentRepository>()
            val songCommentRepository = mockk<SongCommentRepository>()

            val service = CatalogApplicationService(
                songRepository,
                categoryRepository,
                tagRepository,
                attachmentRepository,
                songCommentRepository
            )

            // Mock filterByCategory to implement correct filtering logic in-memory
            every { songRepository.filterByCategory(any(), any(), eq(churchId)) } answers {
                val catId = firstArg<UUID?>()
                val tIds = secondArg<List<UUID>>()
                allSongs.filter { song ->
                    val matchesCat = catId == null ||
                        song.categories.any { it.id == catId }
                    val matchesT = tIds.isEmpty() ||
                        song.tags.any { it.id in tIds }
                    matchesCat && matchesT
                }
            }

            // Call the service filter method
            val result = service.filterSongs(filterCategoryId, filterTagIds, churchId)

            // Property assertion 1: Every song in the result belongs to the selected category (if specified)
            if (filterCategoryId != null) {
                result.forEach { song ->
                    song.categories.any { it.id == filterCategoryId } shouldBe true
                }
            }

            // Property assertion 2: Every song in the result has at least one of the selected tags (if specified)
            if (filterTagIds.isNotEmpty()) {
                result.forEach { song ->
                    song.tags.any { it.id in filterTagIds } shouldBe true
                }
            }

            // Property assertion 3: No song matching both criteria is excluded from the result
            result.map { it.id }.toSet() shouldBe expectedSongs.map { it.id }.toSet()
        }
    }
})
