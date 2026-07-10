CREATE TABLE connections (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    addressee_id BIGINT NOT NULL,
    user_low_id BIGINT NOT NULL,
    user_high_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_connections_requester
        FOREIGN KEY (requester_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_connections_addressee
        FOREIGN KEY (addressee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_connections_user_pair
        UNIQUE (user_low_id, user_high_id),

    CONSTRAINT chk_connections_different_users
        CHECK (requester_id <> addressee_id)
);
