CREATE TABLE saved_jobs (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    job_id     BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_saved_jobs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_saved_jobs_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT uk_saved_jobs_user_job UNIQUE (user_id, job_id)
);
