-- Add push notification tables for device token registration and notification preferences
-- Validates: Requirements 1.2, 1.7, 11.4

-- Device tokens table: stores FCM tokens for push notification delivery
CREATE TABLE device_tokens (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

-- Index for efficient lookup of tokens by user
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);

-- Notification preferences table: per-user push notification preferences
CREATE TABLE notification_preferences (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    service_assignments BOOLEAN NOT NULL DEFAULT TRUE,
    chat_messages BOOLEAN NOT NULL DEFAULT TRUE,
    song_comments BOOLEAN NOT NULL DEFAULT TRUE,
    team_changes BOOLEAN NOT NULL DEFAULT TRUE,
    new_songs BOOLEAN NOT NULL DEFAULT TRUE,
    service_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    invitation_responses BOOLEAN NOT NULL DEFAULT TRUE,
    setlist_changes BOOLEAN NOT NULL DEFAULT TRUE,
    service_cancellations BOOLEAN NOT NULL DEFAULT TRUE,
    recurring_services BOOLEAN NOT NULL DEFAULT TRUE,
    song_updates BOOLEAN NOT NULL DEFAULT TRUE,
    song_deletions BOOLEAN NOT NULL DEFAULT TRUE,
    song_attachments BOOLEAN NOT NULL DEFAULT TRUE,
    invitation_accepted BOOLEAN NOT NULL DEFAULT TRUE,
    availability_changes BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_preferences_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);
