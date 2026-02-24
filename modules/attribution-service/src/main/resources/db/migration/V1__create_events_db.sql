DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'attribution_source') THEN
            CREATE TYPE attribution_source AS ENUM ('Facebook','Google','Other');
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS events (
    conversion_action VARCHAR(32) NOT NULL,
    event_id UUID NOT NULL,
    user_id VARCHAR(32) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    source attribution_source,
    amount DECIMAL(19, 4) NOT NULL,
    PRIMARY KEY (conversion_action, event_id)
);