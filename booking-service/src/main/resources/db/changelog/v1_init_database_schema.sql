-- liquibase formatted sql

-- changeset Admin:001

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE TABLE schedules
(
    id         UUID DEFAULT gen_random_uuid() NOT NULL,
    tutor_id   UUID NOT NULL,
    subject_id UUID NOT NULL,
    date       DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    is_booked  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_schedules PRIMARY KEY (id)
);

-- changeset Admin:002
CREATE TABLE booking
(
    id            UUID DEFAULT gen_random_uuid() NOT NULL,
    user_id       UUID NOT NULL,
    schedule_id   UUID NOT NULL,
    booking_time  TIMESTAMP NOT NULL,
    status        VARCHAR(255) NOT NULL,
    CONSTRAINT pk_booking PRIMARY KEY (id),
    CONSTRAINT fk_booking_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id) ON DELETE CASCADE,
    CONSTRAINT uc_booking_schedule UNIQUE (schedule_id)
);
