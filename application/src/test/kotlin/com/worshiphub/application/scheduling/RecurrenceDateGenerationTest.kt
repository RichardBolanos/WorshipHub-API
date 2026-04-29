package com.worshiphub.application.scheduling

import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.scheduling.RecurrenceFrequency
import com.worshiphub.domain.scheduling.RecurrenceRule
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for generateRecurrenceDates in SchedulingApplicationService.
 * Tests frequency calculations and day-31-in-short-months edge cases.
 */
class RecurrenceDateGenerationTest {

    private val service = SchedulingApplicationService(
        serviceEventRepository = mockk<ServiceEventRepository>(),
        setlistRepository = mockk<SetlistRepository>(),
        userAvailabilityRepository = mockk<UserAvailabilityRepository>(),
        teamRepository = mockk<TeamRepository>(),
        userRepository = mockk<UserRepository>()
    )

    // ── WEEKLY frequency ──

    @Test
    fun `WEEKLY should generate dates every 7 days`() {
        val start = LocalDateTime.of(2025, 1, 5, 10, 0) // Sunday
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 2, 2))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(4, dates.size)
        assertEquals(LocalDateTime.of(2025, 1, 12, 10, 0), dates[0])
        assertEquals(LocalDateTime.of(2025, 1, 19, 10, 0), dates[1])
        assertEquals(LocalDateTime.of(2025, 1, 26, 10, 0), dates[2])
        assertEquals(LocalDateTime.of(2025, 2, 2, 10, 0), dates[3])
    }

    @Test
    fun `WEEKLY with no end date should default to 52 weeks horizon`() {
        val start = LocalDateTime.of(2025, 1, 1, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, null)

        val dates = service.generateRecurrenceDates(start, rule)

        // 52 weeks from Jan 1 = Dec 31, so we should get 52 instances
        assertEquals(52, dates.size)
        // First date should be 1 week after start
        assertEquals(LocalDateTime.of(2025, 1, 8, 10, 0), dates[0])
        // Last date should be within 52 weeks
        assertTrue(dates.last().toLocalDate() <= start.toLocalDate().plusWeeks(52))
    }

    // ── MONTHLY frequency ──

    @Test
    fun `MONTHLY should generate dates every month`() {
        val start = LocalDateTime.of(2025, 1, 15, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 4, 15))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(3, dates.size)
        assertEquals(LocalDateTime.of(2025, 2, 15, 10, 0), dates[0])
        assertEquals(LocalDateTime.of(2025, 3, 15, 10, 0), dates[1])
        assertEquals(LocalDateTime.of(2025, 4, 15, 10, 0), dates[2])
    }

    @Test
    fun `MONTHLY with day 31 on February should use Feb 28 in non-leap year`() {
        val start = LocalDateTime.of(2025, 1, 31, 10, 0) // Jan 31
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 3, 31))

        val dates = service.generateRecurrenceDates(start, rule)

        // Feb has 28 days in 2025 (non-leap), Mar has 31
        assertEquals(2, dates.size)
        assertEquals(LocalDateTime.of(2025, 2, 28, 10, 0), dates[0]) // Feb 28
        assertEquals(LocalDateTime.of(2025, 3, 31, 10, 0), dates[1]) // Mar 31
    }

    @Test
    fun `MONTHLY with day 31 on February should use Feb 29 in leap year`() {
        val start = LocalDateTime.of(2024, 1, 31, 10, 0) // Jan 31, 2024 is leap year
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2024, 3, 31))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(2, dates.size)
        assertEquals(LocalDateTime.of(2024, 2, 29, 10, 0), dates[0]) // Feb 29 (leap)
        assertEquals(LocalDateTime.of(2024, 3, 31, 10, 0), dates[1]) // Mar 31
    }

    @Test
    fun `MONTHLY with day 31 on April should use April 30`() {
        val start = LocalDateTime.of(2025, 3, 31, 10, 0) // Mar 31
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 5, 31))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(2, dates.size)
        assertEquals(LocalDateTime.of(2025, 4, 30, 10, 0), dates[0]) // Apr 30
        assertEquals(LocalDateTime.of(2025, 5, 31, 10, 0), dates[1]) // May 31
    }

    @Test
    fun `MONTHLY with day 30 on February should use Feb 28`() {
        val start = LocalDateTime.of(2025, 1, 30, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 3, 30))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(2, dates.size)
        assertEquals(LocalDateTime.of(2025, 2, 28, 10, 0), dates[0]) // Feb 28
        assertEquals(LocalDateTime.of(2025, 3, 30, 10, 0), dates[1]) // Mar 30
    }

    // ── YEARLY frequency ──

    @Test
    fun `YEARLY should generate dates every year`() {
        val start = LocalDateTime.of(2025, 6, 15, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.YEARLY, LocalDate.of(2028, 6, 15))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(3, dates.size)
        assertEquals(LocalDateTime.of(2026, 6, 15, 10, 0), dates[0])
        assertEquals(LocalDateTime.of(2027, 6, 15, 10, 0), dates[1])
        assertEquals(LocalDateTime.of(2028, 6, 15, 10, 0), dates[2])
    }

    @Test
    fun `YEARLY with no end date should default to 52 weeks horizon`() {
        val start = LocalDateTime.of(2025, 1, 1, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.YEARLY, null)

        val dates = service.generateRecurrenceDates(start, rule)

        // 52 weeks from Jan 1 2025 = ~Dec 31 2025, so only 0 yearly instances
        // because Jan 1 2026 is beyond 52 weeks from Jan 1 2025
        assertEquals(0, dates.size)
    }

    // ── Edge cases ──

    @Test
    fun `should preserve time component across all generated dates`() {
        val start = LocalDateTime.of(2025, 1, 5, 14, 30)
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 1, 26))

        val dates = service.generateRecurrenceDates(start, rule)

        dates.forEach { date ->
            assertEquals(14, date.hour)
            assertEquals(30, date.minute)
        }
    }

    @Test
    fun `should return empty list when end date equals start date`() {
        val start = LocalDateTime.of(2025, 1, 5, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 1, 5))

        val dates = service.generateRecurrenceDates(start, rule)

        assertEquals(0, dates.size)
    }

    @Test
    fun `should not include start date in generated dates`() {
        val start = LocalDateTime.of(2025, 1, 5, 10, 0)
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 2, 2))

        val dates = service.generateRecurrenceDates(start, rule)

        assertFalse(dates.contains(start))
    }
}
