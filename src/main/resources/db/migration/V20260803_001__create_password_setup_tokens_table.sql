CREATE TABLE password_setup_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,

    CONSTRAINT fk_password_setup_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);
