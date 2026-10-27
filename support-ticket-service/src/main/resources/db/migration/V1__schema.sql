CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    document VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    corporate_email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    location VARCHAR(100),
    area VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS technicians (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    document VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    location VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    max_active_tickets INT NOT NULL DEFAULT 5
);

CREATE TABLE IF NOT EXISTS technician_specialties (
    technician_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    PRIMARY KEY (technician_id, specialty),
    CONSTRAINT fk_tech_specialties_technician FOREIGN KEY (technician_id) REFERENCES technicians(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(corporate_email);
CREATE INDEX IF NOT EXISTS idx_technicians_code ON technicians(code);
