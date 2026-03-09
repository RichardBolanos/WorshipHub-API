-- V8: Redesign categories and tags to many-to-many relationship with songs
-- This migration fixes the incorrect one-to-many relationship

-- Step 1: Create new many-to-many junction tables
CREATE TABLE song_categories (
    song_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (song_id, category_id)
);

CREATE TABLE song_tags (
    song_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (song_id, tag_id)
);

-- Step 2: Migrate existing data if any
INSERT INTO song_categories (song_id, category_id)
SELECT song_id, id FROM categories WHERE song_id IS NOT NULL;

INSERT INTO song_tags (song_id, tag_id)
SELECT song_id, id FROM tags WHERE song_id IS NOT NULL;

-- Step 3: Drop old columns
ALTER TABLE categories DROP COLUMN IF EXISTS song_id;
ALTER TABLE tags DROP COLUMN IF EXISTS song_id;

-- Step 4: Add description column to categories if not exists
ALTER TABLE categories ADD COLUMN IF NOT EXISTS description VARCHAR(200);

-- Step 5: Add color column to tags if not exists
ALTER TABLE tags ADD COLUMN IF NOT EXISTS color VARCHAR(7);

-- Step 6: Add indexes for performance
CREATE INDEX idx_song_categories_song ON song_categories(song_id);
CREATE INDEX idx_song_categories_category ON song_categories(category_id);
CREATE INDEX idx_song_tags_song ON song_tags(song_id);
CREATE INDEX idx_song_tags_tag ON song_tags(tag_id);

-- Step 7: Add foreign key constraints
ALTER TABLE song_categories
    ADD CONSTRAINT fk_song_categories_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_song_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;

ALTER TABLE song_tags
    ADD CONSTRAINT fk_song_tags_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_song_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;
