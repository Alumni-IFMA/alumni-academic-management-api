CREATE TABLE campuses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL
);

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(50) NOT NULL,
    modality VARCHAR(30) NOT NULL
);

CREATE TABLE campus_course (
    id BIGSERIAL PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,

    CONSTRAINT fk_campus
        FOREIGN KEY (campus_id)
        REFERENCES campuses(id),

    CONSTRAINT fk_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(200) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    bio TEXT,
    profile_picture_url TEXT,
    linkedin_url TEXT,
    portfolio_url TEXT,
    current_position TEXT,
    account_status VARCHAR(50) NOT NULL
);

CREATE TABLE academic_profiles (
    id BIGSERIAL PRIMARY KEY,
    entry_year INTEGER NOT NULL,
    conclusion_year INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    campus_course_id BIGINT,

    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_campus_course
        FOREIGN KEY (campus_course_id)
        REFERENCES campus_course(id)
);