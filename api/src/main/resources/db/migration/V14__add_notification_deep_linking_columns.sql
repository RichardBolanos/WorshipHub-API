-- Add deep linking columns to notifications table
-- These optional fields allow notifications to reference a specific entity
-- for navigation when the user taps the notification.

ALTER TABLE notifications ADD COLUMN related_entity_id UUID;
ALTER TABLE notifications ADD COLUMN related_entity_type VARCHAR(50);

-- Index for efficient lookup by entity
CREATE INDEX idx_notifications_related_entity ON notifications (related_entity_id) WHERE related_entity_id IS NOT NULL;
