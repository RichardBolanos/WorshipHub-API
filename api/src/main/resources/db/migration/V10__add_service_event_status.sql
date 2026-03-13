-- V10: Add status column to service_events table
-- This column tracks the lifecycle of a service event (DRAFT, PUBLISHED, CONFIRMED, CANCELLED)

ALTER TABLE service_events 
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Add constraint to ensure valid status values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_service_event_status'
    ) THEN
        ALTER TABLE service_events 
        ADD CONSTRAINT chk_service_event_status 
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CONFIRMED', 'CANCELLED'));
    END IF;
END $$;

-- Create index for filtering by status
CREATE INDEX IF NOT EXISTS idx_service_events_status ON service_events(status);

-- Create index for finding upcoming services
CREATE INDEX IF NOT EXISTS idx_service_events_scheduled_date ON service_events(scheduled_date);

-- Create composite index for church + status queries
CREATE INDEX IF NOT EXISTS idx_service_events_church_status ON service_events(church_id, status);
