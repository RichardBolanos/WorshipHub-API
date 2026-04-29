package com.worshiphub.domain.scheduling

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

/**
 * Unit tests for RecurrenceRule value object.
 * Validates frequency values and basic construction.
 */
class RecurrenceRuleTest {

    @Test
    fun `should create RecurrenceRule with WEEKLY frequency`() {
        val rule = RecurrenceRule(
            frequency = RecurrenceFrequency.WEEKLY,
            recurrenceEndDate = LocalDate.of(2025, 6, 30)
        )
        assertEquals(RecurrenceFrequency.WEEKLY, rule.frequency)
        assertEquals(LocalDate.of(2025, 6, 30), rule.recurrenceEndDate)
    }

    @Test
    fun `should create RecurrenceRule with MONTHLY frequency`() {
        val rule = RecurrenceRule(
            frequency = RecurrenceFrequency.MONTHLY,
            recurrenceEndDate = LocalDate.of(2025, 12, 31)
        )
        assertEquals(RecurrenceFrequency.MONTHLY, rule.frequency)
    }

    @Test
    fun `should create RecurrenceRule with YEARLY frequency`() {
        val rule = RecurrenceRule(
            frequency = RecurrenceFrequency.YEARLY,
            recurrenceEndDate = null
        )
        assertEquals(RecurrenceFrequency.YEARLY, rule.frequency)
        assertNull(rule.recurrenceEndDate)
    }

    @Test
    fun `should create RecurrenceRule with null end date`() {
        val rule = RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY)
        assertNull(rule.recurrenceEndDate)
    }

    @Test
    fun `should support all three RecurrenceFrequency values`() {
        val frequencies = RecurrenceFrequency.entries
        assertEquals(3, frequencies.size)
        assertTrue(frequencies.contains(RecurrenceFrequency.WEEKLY))
        assertTrue(frequencies.contains(RecurrenceFrequency.MONTHLY))
        assertTrue(frequencies.contains(RecurrenceFrequency.YEARLY))
    }

    @Test
    fun `should implement value equality`() {
        val rule1 = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 6, 30))
        val rule2 = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 6, 30))
        assertEquals(rule1, rule2)
        assertEquals(rule1.hashCode(), rule2.hashCode())
    }

    @Test
    fun `should not be equal with different frequency`() {
        val rule1 = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 6, 30))
        val rule2 = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 6, 30))
        assertNotEquals(rule1, rule2)
    }

    @Test
    fun `should not be equal with different end date`() {
        val rule1 = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 6, 30))
        val rule2 = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 7, 31))
        assertNotEquals(rule1, rule2)
    }
}
