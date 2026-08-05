CREATE TABLE degrees (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    file_url   VARCHAR(500) NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_degrees_user FOREIGN KEY (user_id) REFERENCES users(id)
);
