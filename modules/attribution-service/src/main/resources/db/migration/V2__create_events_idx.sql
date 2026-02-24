--! flyway:transactional=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_events_timestamp ON events(timestamp);