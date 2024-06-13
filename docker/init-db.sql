-- CREATE
--     USER app WITH PASSWORD 'pass';
--
-- CREATE
--     DATABASE async_adapter;
--
-- \c async_adapter;

CREATE SCHEMA async_adapter;
ALTER
    DATABASE async_adapter SET search_path TO async_adapter;

GRANT ALL PRIVILEGES ON DATABASE
    async_adapter TO app;
GRANT ALL PRIVILEGES ON SCHEMA
    async_adapter TO app;
GRANT ALL PRIVILEGES ON ALL
    TABLES IN SCHEMA async_adapter TO app;

CREATE TABLE IF NOT EXISTS async_adapter.score_details
(
    id        BIGSERIAL PRIMARY KEY,
    subject   VARCHAR(255)     NOT NULL,
    avg_score DOUBLE PRECISION NOT NULL
);