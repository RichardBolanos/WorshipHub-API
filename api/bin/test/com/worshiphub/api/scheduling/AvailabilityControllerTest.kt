package com.worshiphub.api.scheduling

import com.worshiphub.application.scheduling.DeleteAvailabilityCommand
import com.worshiphub.application.scheduling.GetMyAvailabilityCommand
import com.worshiphub.application.scheduling.SchedulingApplicationService
import com.worshiphub.api.common.ForbiddenException
import com.worshiphub.api.common.NotFoundException
import com.worshiphub.domain.scheduling.UserAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for AvailabilityController.
 * Tests controller logic directly without Spring context to avoid
 * compilation issues with other broken test files in the api module.
 */
class AvailabilityControllerTest {

    private val schedulingApplicationService = mockk<SchedulingApplicationService>()
    private val controller = AvailabilityController(schedulingApplicationService)

    private val userId: UUID = UUID.randomUUID()
    private val availabilityId: UUID = UUID.randomUUID()

    // ── DELETE /api/v1/services/availability/{id} → 204 ──

    @Test
    fun `deleteAvailability should succeed silently on success (204)`() {
        every { schedulingApplicationService.deleteAvailability(any()) } returns Result.success(Unit)

        // Should not throw
        controller.deleteAvailability(availabilityId, userId)

        val commandSlot = slot<DeleteAvailabilityCommand>()
        verify { schedulingApplicationService.deleteAvailability(capture(commandSlot)) }
        assertEquals(availabilityId, commandSlot.captured.availabilityId)
        assertEquals(userId, commandSlot.captured.userId)
    }

    // ── DELETE /api/v1/services/availability/{id} → 404 ──

    @Test
    fun `deleteAvailability should throw NotFoundException when not found`() {
        every { schedulingApplicationService.deleteAvailability(any()) } returns
            Result.failure(NoSuchElementException("Registro de indisponibilidad no encontrado"))

        val exception = assertThrows<NotFoundException> {
            controller.deleteAvailability(availabilityId, userId)
        }
        assertTrue(exception.message?.contains("no encontrado") == true)
    }

    // ── DELETE /api/v1/services/availability/{id} → 403 ──

    @Test
    fun `deleteAvailability should throw ForbiddenException when wrong user`() {
        every { schedulingApplicationService.deleteAvailability(any()) } returns
            Result.failure(SecurityException("No tiene permiso para eliminar este registro"))

        val exception = assertThrows<ForbiddenException> {
            controller.deleteAvailability(availabilityId, userId)
        }
        assertTrue(exception.message?.contains("permiso") == true)
    }

    @Test
    fun `deleteAvailability should throw RuntimeException on unexpected error`() {
        every { schedulingApplicationService.deleteAvailability(any()) } returns
            Result.failure(RuntimeException("Unexpected error"))

        assertThrows<RuntimeException> {
            controller.deleteAvailability(availabilityId, userId)
        }
    }

    // ── GET /api/v1/services/availability/me ──

    @Test
    fun `getMyAvailability should return list of availability records`() {
        val record1Id = UUID.randomUUID()
        val record2Id = UUID.randomUUID()
        val records = listOf(
            UserAvailability(
                id = record1Id,
                userId = userId,
                unavailableDate = LocalDate.of(2025, 1, 15),
                reason = "Viaje familiar",
                createdAt = LocalDateTime.of(2025, 1, 10, 14, 30)
            ),
            UserAvailability(
                id = record2Id,
                userId = userId,
                unavailableDate = LocalDate.of(2025, 1, 22),
                reason = null,
                createdAt = LocalDateTime.of(2025, 1, 11, 9, 0)
            )
        )

        every { schedulingApplicationService.getMyAvailability(any()) } returns records

        val result = controller.getMyAvailability(userId, null, null)

        assertEquals(2, result.size)
        assertEquals(record1Id, result[0].id)
        assertEquals(LocalDate.of(2025, 1, 15), result[0].unavailableDate)
        assertEquals("Viaje familiar", result[0].reason)
        assertEquals(record2Id, result[1].id)
        assertEquals(LocalDate.of(2025, 1, 22), result[1].unavailableDate)
        assertNull(result[1].reason)
    }

    @Test
    fun `getMyAvailability should return correct fields in response`() {
        val recordId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2025, 2, 1, 8, 0)
        val records = listOf(
            UserAvailability(
                id = recordId,
                userId = userId,
                unavailableDate = LocalDate.of(2025, 2, 10),
                reason = "Conferencia",
                createdAt = createdAt
            )
        )

        every { schedulingApplicationService.getMyAvailability(any()) } returns records

        val result = controller.getMyAvailability(userId, null, null)

        assertEquals(1, result.size)
        val response = result[0]
        assertEquals(recordId, response.id)
        assertEquals(LocalDate.of(2025, 2, 10), response.unavailableDate)
        assertEquals("Conferencia", response.reason)
        assertEquals(createdAt, response.createdAt)
    }

    @Test
    fun `getMyAvailability should pass date filters to service`() {
        val commandSlot = slot<GetMyAvailabilityCommand>()
        every { schedulingApplicationService.getMyAvailability(capture(commandSlot)) } returns emptyList()

        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        controller.getMyAvailability(userId, startDate, endDate)

        val captured = commandSlot.captured
        assertEquals(userId, captured.userId)
        assertEquals(startDate, captured.startDate)
        assertEquals(endDate, captured.endDate)
    }

    @Test
    fun `getMyAvailability should pass null dates when no filters`() {
        val commandSlot = slot<GetMyAvailabilityCommand>()
        every { schedulingApplicationService.getMyAvailability(capture(commandSlot)) } returns emptyList()

        controller.getMyAvailability(userId, null, null)

        val captured = commandSlot.captured
        assertEquals(userId, captured.userId)
        assertNull(captured.startDate)
        assertNull(captured.endDate)
    }

    @Test
    fun `getMyAvailability should return empty list when no records`() {
        every { schedulingApplicationService.getMyAvailability(any()) } returns emptyList()

        val result = controller.getMyAvailability(userId, null, null)

        assertTrue(result.isEmpty())
    }
}
