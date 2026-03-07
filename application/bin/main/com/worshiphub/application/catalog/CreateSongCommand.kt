package com.worshiphub.application.catalog

import java.util.*

/** Command for creating a new song. */
data class CreateSongCommand(
        val title: String,
        val artist: String?,
        val key: String?,
        val bpm: Int?,
        val lyrics: String?,
        val chords: String?,
        val churchId: UUID
)
