-- liquibase formatted sql

-- changeset Admin:001

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE users
(
    id       UUID DEFAULT uuid_generate_v4() NOT NULL,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email)
);

-- changeset Admin:002
CREATE TABLE subjects
(
    id         UUID DEFAULT uuid_generate_v4() NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_subjects PRIMARY KEY (id)
);

-- changeset Admin:003
CREATE TABLE tutors
(
    id         UUID DEFAULT uuid_generate_v4() NOT NULL,
    name       VARCHAR(255) NOT NULL,
    subject_id UUID NOT NULL,
    CONSTRAINT pk_tutors PRIMARY KEY (id),
    CONSTRAINT fk_tutors_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);

-- changeset Admin:004
CREATE TABLE user_subjects
(
    user_id    UUID NOT NULL,
    subject_id UUID NOT NULL,
    CONSTRAINT pk_user_subjects PRIMARY KEY (user_id, subject_id),
    CONSTRAINT fk_user_subjects_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);