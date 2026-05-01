package com.worshiphub.application.catalog

import java.util.*

data class UpdateSongCommand(
        val title: String,
        val artist: String? = null,
        val key: String? = null,
        val bpm: Int? = null,
        val lyrics: String? = null,
        val chords: String? = null,
        val tagIds: List<UUID>? = null,
        val categoryIds: List<UUID>? = null
)
