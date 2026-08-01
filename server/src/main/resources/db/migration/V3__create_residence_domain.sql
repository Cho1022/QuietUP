CREATE TABLE apartment_complexes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    road_address VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_apartment_complexes_name_address UNIQUE (name, road_address)
);

CREATE TABLE apartment_buildings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    apartment_complex_id BIGINT NOT NULL,
    building_number VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_apartment_buildings_complex_number UNIQUE (apartment_complex_id, building_number),
    CONSTRAINT fk_apartment_buildings_complex FOREIGN KEY (apartment_complex_id) REFERENCES apartment_complexes (id)
);

CREATE TABLE apartment_units (
    id BIGINT NOT NULL AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    unit_number VARCHAR(20) NOT NULL,
    floor_number INT NOT NULL,
    line_number INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_apartment_units_building_number UNIQUE (building_id, unit_number),
    CONSTRAINT uk_apartment_units_building_floor_line UNIQUE (building_id, floor_number, line_number),
    CONSTRAINT fk_apartment_units_building FOREIGN KEY (building_id) REFERENCES apartment_buildings (id)
);

CREATE TABLE residence_verification_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    unit_id BIGINT NOT NULL,
    code_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    used_by_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_residence_verification_codes_hash UNIQUE (code_hash),
    CONSTRAINT fk_residence_verification_codes_unit FOREIGN KEY (unit_id) REFERENCES apartment_units (id),
    CONSTRAINT fk_residence_verification_codes_user FOREIGN KEY (used_by_user_id) REFERENCES users (id),
    INDEX idx_residence_verification_codes_unit_id (unit_id),
    INDEX idx_residence_verification_codes_expires_at (expires_at)
);

CREATE TABLE residences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    verified_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_residences_user_id UNIQUE (user_id),
    CONSTRAINT fk_residences_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_residences_unit FOREIGN KEY (unit_id) REFERENCES apartment_units (id),
    INDEX idx_residences_unit_id (unit_id)
);
