-- V11: Add missing columns to songs table
-- Adds duration column that was missing from the initial schema

ALTER TABLE songs 
ADD COLUMN IF NOT EXISTS duration INTEGER;

-- Add comment to clarify the column purpose
COMMENT ON COLUMN songs.duration IS 'Estimated duration of the song in minutes';
