-- V6: Add lyrics column to songs table
-- Description: Allows storing full song lyrics alongside ChordPro chords

ALTER TABLE songs ADD COLUMN IF NOT EXISTS lyrics TEXT;
