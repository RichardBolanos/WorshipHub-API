-- V9: Add error_logs table for intelligent error tracking
-- This table stores aggregated error information to avoid duplicate logging

CREATE TABLE IF NOT EXISTS error_logs (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    error_hash VARCHAR(255) UNIQUE NOT NULL,
    error_message TEXT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    line_number INTEGER NOT NULL,
    stack_trace TEXT,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    first_occurrence TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_occurrence TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster lookups by hash
CREATE INDEX IF NOT EXISTS idx_error_logs_hash ON error_logs(error_hash);

-- Create index for querying recent errors
CREATE INDEX IF NOT EXISTS idx_error_logs_last_occurrence ON error_logs(last_occurrence DESC);

-- Create index for finding most frequent errors
CREATE INDEX IF NOT EXISTS idx_error_logs_occurrence_count ON error_logs(occurrence_count DESC);
