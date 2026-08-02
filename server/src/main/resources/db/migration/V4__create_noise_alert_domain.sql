CREATE TABLE noise_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    apartment_complex_id BIGINT NOT NULL,
    sender_residence_id BIGINT NOT NULL,
    target_unit_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    noise_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,
    resolved_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_noise_alerts_apartment_complex FOREIGN KEY (apartment_complex_id)
        REFERENCES apartment_complexes (id),
    CONSTRAINT fk_noise_alerts_sender_residence FOREIGN KEY (sender_residence_id)
        REFERENCES residences (id),
    CONSTRAINT fk_noise_alerts_target_unit FOREIGN KEY (target_unit_id)
        REFERENCES apartment_units (id),
    INDEX idx_noise_alerts_sender_created (sender_residence_id, created_at),
    INDEX idx_noise_alerts_target_created (target_unit_id, created_at),
    INDEX idx_noise_alerts_apartment_created (apartment_complex_id, created_at)
);

CREATE TABLE noise_alert_responses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    noise_alert_id BIGINT NOT NULL,
    responder_residence_id BIGINT NOT NULL,
    response_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_noise_alert_responses_alert_id UNIQUE (noise_alert_id),
    CONSTRAINT fk_noise_alert_responses_alert FOREIGN KEY (noise_alert_id)
        REFERENCES noise_alerts (id),
    CONSTRAINT fk_noise_alert_responses_responder FOREIGN KEY (responder_residence_id)
        REFERENCES residences (id)
);
