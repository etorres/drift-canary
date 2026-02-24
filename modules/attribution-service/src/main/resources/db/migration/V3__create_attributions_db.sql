DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'attribution_channel') THEN
            CREATE TYPE attribution_channel AS ENUM ('Organic','PaidSearch','PaidSocial');
        END IF;
    END
$$;

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'model_version') THEN
            CREATE TYPE model_version AS ENUM ('v1','v2');
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS attributions (
    conversion_action VARCHAR(32) NOT NULL,
    event_id UUID NOT NULL,
    channel attribution_channel,
    version model_version,
    PRIMARY KEY (conversion_action, event_id),
    FOREIGN KEY (conversion_action, event_id)
        REFERENCES events(conversion_action, event_id)
        ON DELETE CASCADE
);