-- V13: Rename recurrence_frequency to frequency in service_events table
-- Hibernate expects 'frequency' from the embedded RecurrenceRule field name

ALTER TABLE service_events
    RENAME COLUMN recurrence_frequency TO frequency;
