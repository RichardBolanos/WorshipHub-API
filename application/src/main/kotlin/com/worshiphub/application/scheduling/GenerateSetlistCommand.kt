package com.worshiphub.application.scheduling

import java.util.*

/**
 * Command for auto-generating a setlist based on rules.
 */
data class GenerateSetlistCommand(
    val name: String,
    val churchId: UUID,
    val rules: SetlistRules
)

/**
 * Rules for setlist generation.
 */
data class SetlistRules(
    val openingSongs: Int = 1,
    val worshipSongs: Int = 2,
    val offeringSongs: Int = 1,
    val closingSongs: Int = 1
)