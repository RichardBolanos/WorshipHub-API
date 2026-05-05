package com.worshiphub.domain.scheduling

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDate

/**
 * Value object representing a recurrence rule for a service event.
 *
 * @property frequency How often the service repeats (WEEKLY, MONTHLY, YEARLY)
 * @property recurrenceEndDate Optional end date for the recurrence series
 */
@Embeddable
data class RecurrenceRule(
    @Enumerated(EnumType.STRING)
    val frequency: RecurrenceFrequency,
    val recurrenceEndDate: LocalDate? = null,
)
