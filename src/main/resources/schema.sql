CREATE SCHEMA IF NOT EXISTS async_adapter;

CREATE TABLE IF NOT EXISTS async_adapter.score_details(
    id        BIGSERIAL PRIMARY KEY,
    subject   VARCHAR(255)     NOT NULL,
    avg_score DOUBLE PRECISION NOT NULL
);