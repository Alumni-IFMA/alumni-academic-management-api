CREATE TABLE news (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    summary         TEXT,
    content         TEXT NOT NULL,
    cover_image_url VARCHAR(500),
    published_at    TIMESTAMP,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL
);
