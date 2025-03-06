CREATE TYPE appointment_type_enum AS ENUM ('INITIAL', 'STANDARD', 'CHECK_IN');

CREATE TABLE patient (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE practitioner (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE practitioner_unavailability (
    id BIGSERIAL PRIMARY KEY,
    practitioner_id BIGINT NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT unique_practitioner_unavailability UNIQUE (practitioner_id, start_at, end_at),
    CONSTRAINT fk_practitioner_unavailability_practitioner FOREIGN KEY (practitioner_id) REFERENCES practitioner(id) ON DELETE CASCADE
);

CREATE INDEX idx_unavailability_practitioner_id_time_range ON practitioner_unavailability (practitioner_id, start_at, end_at);
CREATE INDEX idx_unavailability_practitioners_time_range ON practitioner_unavailability (start_at, end_at);

CREATE TABLE appointment (
    id BIGSERIAL PRIMARY KEY,
    appointment_type appointment_type_enum NOT NULL,
    patient_id BIGINT NOT NULL,
    practitioner_id BIGINT NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT unique_practitioner_schedule UNIQUE (practitioner_id, start_at, end_at),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_appointment_practitioner FOREIGN KEY (practitioner_id) REFERENCES practitioner(id)
);

CREATE INDEX idx_appointment_practitioner_id_time_range ON appointment (practitioner_id, start_at, end_at);
CREATE INDEX idx_appointment_id_time_range ON appointment (appointment_type, start_at, end_at);