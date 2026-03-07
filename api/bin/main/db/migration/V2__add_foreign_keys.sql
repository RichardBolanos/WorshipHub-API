-- Add foreign key constraints

-- Users foreign keys
ALTER TABLE users ADD CONSTRAINT fk_users_church_id FOREIGN KEY (church_id) REFERENCES churches(id);

-- Teams foreign keys
ALTER TABLE teams ADD CONSTRAINT fk_teams_church_id FOREIGN KEY (church_id) REFERENCES churches(id);
ALTER TABLE teams ADD CONSTRAINT fk_teams_leader_id FOREIGN KEY (leader_id) REFERENCES users(id);

-- Team members foreign keys
ALTER TABLE team_members ADD CONSTRAINT fk_team_members_team_id FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE team_members ADD CONSTRAINT fk_team_members_user_id FOREIGN KEY (user_id) REFERENCES users(id);

-- Songs foreign keys
ALTER TABLE songs ADD CONSTRAINT fk_songs_church_id FOREIGN KEY (church_id) REFERENCES churches(id);

-- Attachments foreign keys
ALTER TABLE attachments ADD CONSTRAINT fk_attachments_song_id FOREIGN KEY (song_id) REFERENCES songs(id);

-- Categories foreign keys
ALTER TABLE categories ADD CONSTRAINT fk_categories_church_id FOREIGN KEY (church_id) REFERENCES churches(id);

-- Tags foreign keys
ALTER TABLE tags ADD CONSTRAINT fk_tags_church_id FOREIGN KEY (church_id) REFERENCES churches(id);

-- Service events foreign keys
ALTER TABLE service_events ADD CONSTRAINT fk_service_events_team_id FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE service_events ADD CONSTRAINT fk_service_events_setlist_id FOREIGN KEY (setlist_id) REFERENCES setlists(id);
ALTER TABLE service_events ADD CONSTRAINT fk_service_events_church_id FOREIGN KEY (church_id) REFERENCES churches(id);

-- Setlists foreign keys
ALTER TABLE setlists ADD CONSTRAINT fk_setlists_church_id FOREIGN KEY (church_id) REFERENCES churches(id);

-- Setlist songs foreign keys
ALTER TABLE setlist_songs ADD CONSTRAINT fk_setlist_songs_setlist_id FOREIGN KEY (setlist_id) REFERENCES setlists(id);
ALTER TABLE setlist_songs ADD CONSTRAINT fk_setlist_songs_song_id FOREIGN KEY (song_id) REFERENCES songs(id);

-- Assigned members foreign keys
ALTER TABLE assigned_members ADD CONSTRAINT fk_assigned_members_service_event_id FOREIGN KEY (service_event_id) REFERENCES service_events(id);
ALTER TABLE assigned_members ADD CONSTRAINT fk_assigned_members_user_id FOREIGN KEY (user_id) REFERENCES users(id);

-- Notifications foreign keys
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id);

-- Chat messages foreign keys
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_team_id FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_user_id FOREIGN KEY (user_id) REFERENCES users(id);

-- Song comments foreign keys
ALTER TABLE song_comments ADD CONSTRAINT fk_song_comments_song_id FOREIGN KEY (song_id) REFERENCES songs(id);
ALTER TABLE song_comments ADD CONSTRAINT fk_song_comments_user_id FOREIGN KEY (user_id) REFERENCES users(id);

-- User availability foreign keys
ALTER TABLE user_availability ADD CONSTRAINT fk_user_availability_user_id FOREIGN KEY (user_id) REFERENCES users(id);