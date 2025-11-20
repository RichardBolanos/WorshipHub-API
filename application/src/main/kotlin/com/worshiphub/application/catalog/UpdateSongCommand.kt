package com.worshiphub.application.catalog

data class UpdateSongCommand(
    val title: String,
    val artist: String,
    val key: String,
    val bpm: Int? = null,
    val chords: String? = null
)