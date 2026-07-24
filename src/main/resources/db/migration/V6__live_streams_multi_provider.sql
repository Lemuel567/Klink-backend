-- Live streams go multi-provider: YouTube OR Facebook Live.
-- V5 stored a YouTube-only `youtube_video_id`; generalise it to
-- (provider, source_ref) so other platforms can be carried without another
-- schema change. V5 is already applied, so this is a follow-up migration
-- rather than an edit (editing V5 would trip Flyway's checksum validation).

ALTER TABLE live_streams ADD COLUMN IF NOT EXISTS provider   VARCHAR(20);
ALTER TABLE live_streams ADD COLUMN IF NOT EXISTS source_ref VARCHAR(600);

-- Everything recorded before this migration could only have been YouTube.
UPDATE live_streams SET provider = 'YOUTUBE' WHERE provider IS NULL;
UPDATE live_streams SET source_ref = youtube_video_id
    WHERE source_ref IS NULL AND youtube_video_id IS NOT NULL;

-- Any row that somehow still lacks a source is unusable — drop it rather than
-- fail the NOT NULL below (defensive; normally matches zero rows).
DELETE FROM live_streams WHERE source_ref IS NULL;

ALTER TABLE live_streams ALTER COLUMN provider   SET NOT NULL;
ALTER TABLE live_streams ALTER COLUMN source_ref SET NOT NULL;

-- Must be dropped, not merely abandoned: it is NOT NULL, so leaving it while
-- the entity no longer maps it would make every future INSERT fail.
ALTER TABLE live_streams DROP COLUMN IF EXISTS youtube_video_id;
