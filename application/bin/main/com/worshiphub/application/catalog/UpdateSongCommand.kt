package com.worshiphub.application.catalog

data class UpdateSongCommand(
        val title: String,
        val artist: String? = null,
        val key: String? = null,
        val bpm: Int? = null,
        val lyrics: String? = null,
        val chords: String? = null
)
