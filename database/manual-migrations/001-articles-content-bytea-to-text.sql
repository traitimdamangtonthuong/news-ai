-- Run this once against the existing `news_ai` PostgreSQL database before
-- enabling keyword search in article content.
--
-- The Article entity declares `content` as TEXT. Older local databases may
-- nevertheless contain it as BYTEA; PostgreSQL cannot apply LOWER/LIKE to
-- BYTEA, which makes keyword search fail. Existing crawler content is UTF-8.

BEGIN;

ALTER TABLE articles
    ALTER COLUMN content TYPE TEXT
    USING convert_from(content, 'UTF8');

COMMIT;
