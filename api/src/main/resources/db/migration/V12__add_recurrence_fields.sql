-- V12: Add recurrence fields to service_events table
-- Supports recurring service events (WEEKLY, MONTHLY, YEARLY) with parent-child relationships
-- Also adds composite index on user_availability for efficient date-range queries

-- Add recurrence frequency column (WEEKLY, MONTHLY, YEARLY)
ALTER TABLE service_events
    ADD COLUMN IF NOT EXISTS recurrence_frequency VARCHAR(10);

-- Add recurrence end date column
ALTER TABLE service_events
    ADD COLUMN IF NOT EXISTS recurrence_end_date DATE;

-- Add parent service ID for linking child instances to the parent recurring event
ALTER TABLE service_events
    ADD COLUMN IF NOT EXISTS parent_service_id UUID REFERENCES service_events(id) ON DELETE SET NULL;

-- Index for querying child instances by parent service ID
CREATE INDEX IF NOT EXISTS idx_service_events_parent_id ON service_events(parent_service_id);

-- Composite index for querying user availability by user and date range
CREATE INDEX IF NOT EXISTS idx_user_availability_user_date ON user_availability(user_id, unavailable_date);
