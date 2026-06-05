CREATE TABLE jobs (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    company          VARCHAR(255) NOT NULL,
    company_logo_url VARCHAR(500),
    description      TEXT NOT NULL,
    location         VARCHAR(255),
    area             VARCHAR(255),
    workplace_type   VARCHAR(50),
    experience_level VARCHAR(50),
    salary           DECIMAL(15, 2),
    external_link    VARCHAR(500),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL
);

CREATE TABLE job_requirements (
    job_id      BIGINT NOT NULL,
    requirement VARCHAR(500) NOT NULL,
    CONSTRAINT fk_job_requirements_job FOREIGN KEY (job_id) REFERENCES jobs(id)
);

CREATE TABLE job_benefits (
    job_id  BIGINT NOT NULL,
    benefit VARCHAR(500) NOT NULL,
    CONSTRAINT fk_job_benefits_job FOREIGN KEY (job_id) REFERENCES jobs(id)
);
