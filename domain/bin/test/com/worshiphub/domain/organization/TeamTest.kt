package com.worshiphub.domain.organization

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

/**
 * Unit tests for Team domain entity.
 */
class TeamTest {
    
    @Test
    fun `should create team with valid data`() {
        val churchId = UUID.randomUUID()
        val team = Team(
            id = UUID.randomUUID(),
            name = "Sunday Morning Team",
            description = "Main worship team",
            churchId = churchId,
            leaderId = UUID.randomUUID()
        )
        
        assertEquals("Sunday Morning Team", team.name)
        assertEquals("Main worship team", team.description)
        assertEquals(churchId, team.churchId)
    }
    
    @Test
    fun `should handle team with minimal data`() {
        val team = Team(
            id = UUID.randomUUID(),
            name = "Youth Team",
            description = null,
            churchId = UUID.randomUUID(),
            leaderId = UUID.randomUUID()
        )
        
        assertEquals("Youth Team", team.name)
        assertNull(team.description)
    }
}