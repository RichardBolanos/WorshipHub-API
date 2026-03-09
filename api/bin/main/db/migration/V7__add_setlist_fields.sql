-- Add missing fields to setlists table
ALTER TABLE setlists ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE setlists ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE setlists ADD COLUMN IF NOT EXISTS event_date TIMESTAMP;

-- Create index for event_date for better query performance
CREATE INDEX IF NOT EXISTS idx_setlists_event_date ON setlists(event_date);
