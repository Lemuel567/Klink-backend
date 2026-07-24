-- Live streams (Tier 1): a church broadcasts on YouTube Live and Klink stores
-- only the metadata needed to embed it in-app. No media is stored here — the
-- video is ingested and delivered entirely by YouTube.
CREATE TABLE IF NOT EXISTS live_streams (
    id               UUID PRIMARY KEY,
    church_id        UUID NOT NULL REFERENCES churches(id),
    title            VARCHAR(200) NOT NULL,
    youtube_video_id VARCHAR(32) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    started_by       UUID,
    started_at       TIMESTAMP NOT NULL,
    ended_at         TIMESTAMP,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);

-- Name matches the entity's @Index so Hibernate ddl-auto:update won't create a
-- duplicate alongside Flyway.
CREATE INDEX IF NOT EXISTS idx_live_streams_church_status
    ON live_streams (church_id, status);
