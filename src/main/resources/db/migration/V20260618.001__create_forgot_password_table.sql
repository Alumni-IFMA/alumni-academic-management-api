CREATE TABLE forgot_password (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,

    CONSTRAINT fk_forgot_password_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);