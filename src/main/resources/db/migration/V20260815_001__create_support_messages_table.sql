CREATE TABLE support_messages (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    message     TEXT NOT NULL,
    resolved    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL,

    CONSTRAINT fk_support_messages_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
