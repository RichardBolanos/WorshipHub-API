-- Add performance indexes

-- Users indexes
CREATE INDEX idx_users_church_id ON users(church_id);
CREATE INDEX idx_users_email ON users(email);

-- Teams indexes
CREATE INDEX idx_teams_church_id ON teams(church_id);
CREATE INDEX idx_teams_leader_id ON teams(leader_id);

-- Team members indexes
CREATE INDEX idx_team_members_team_id ON team_members(team_id);
CREATE INDEX idx_team_members_user_id ON team_members(user_id);

-- Songs indexes
CREATE INDEX idx_songs_church_id ON songs(church_id);
CREATE INDEX idx_songs_title ON songs(title);

-- Service events indexes
CREATE INDEX idx_service_events_team_id ON service_events(team_id);
CREATE INDEX idx_service_events_church_id ON service_events(church_id);
CREATE INDEX idx_service_events_scheduled_date ON service_events(scheduled_date);

-- Assigned members indexes
CREATE INDEX idx_assigned_members_service_event_id ON assigned_members(service_event_id);
CREATE INDEX idx_assigned_members_user_id ON assigned_members(user_id);

-- Notifications indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

-- Chat messages indexes
CREATE INDEX idx_chat_messages_team_id ON chat_messages(team_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);

-- User availability indexes
CREATE INDEX idx_user_availability_user_id ON user_availability(user_id);
CREATE INDEX idx_user_availability_date ON user_availability(unavailable_date);