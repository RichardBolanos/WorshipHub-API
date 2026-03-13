-- V8: Redesign categories and tags to many-to-many relationship with songs
-- This migration creates the many-to-many junction tables

-- Step 1: Create new many-to-many junction tables if they don't exist
CREATE TABLE IF NOT EXISTS song_categories (
    song_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (song_id, category_id)
);

CREATE TABLE IF NOT EXISTS song_tags (
    song_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (song_id, tag_id)
);

-- Step 2: Add description column to categories if not exists
ALTER TABLE categories ADD COLUMN IF NOT EXISTS description VARCHAR(200);

-- Step 3: Add color column to tags if not exists
ALTER TABLE tags ADD COLUMN IF NOT EXISTS color VARCHAR(7);

-- Step 4: Add indexes for performance (only if they don't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_song_categories_song') THEN
        CREATE INDEX idx_song_categories_song ON song_categories(song_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_song_categories_category') THEN
        CREATE INDEX idx_song_categories_category ON song_categories(category_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_song_tags_song') THEN
        CREATE INDEX idx_song_tags_song ON song_tags(song_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_song_tags_tag') THEN
        CREATE INDEX idx_song_tags_tag ON song_tags(tag_id);
    END IF;
END $$;

-- Step 5: Add foreign key constraints (only if they don't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_song_categories_song') THEN
        ALTER TABLE song_categories
            ADD CONSTRAINT fk_song_categories_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_song_categories_category') THEN
        ALTER TABLE song_categories
            ADD CONSTRAINT fk_song_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_song_tags_song') THEN
        ALTER TABLE song_tags
            ADD CONSTRAINT fk_song_tags_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_song_tags_tag') THEN
        ALTER TABLE song_tags
            ADD CONSTRAINT fk_song_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;
    END IF;
END $$;

