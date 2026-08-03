CREATE TABLE chat_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    noise_alert_id BIGINT NOT NULL,
    alert_sender_residence_id BIGINT NOT NULL,
    alert_responder_residence_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_rooms_noise_alert_id UNIQUE (noise_alert_id),
    CONSTRAINT fk_chat_rooms_noise_alert FOREIGN KEY (noise_alert_id)
        REFERENCES noise_alerts (id),
    CONSTRAINT fk_chat_rooms_alert_sender FOREIGN KEY (alert_sender_residence_id)
        REFERENCES residences (id),
    CONSTRAINT fk_chat_rooms_alert_responder FOREIGN KEY (alert_responder_residence_id)
        REFERENCES residences (id),
    INDEX idx_chat_rooms_sender_opened (alert_sender_residence_id, opened_at),
    INDEX idx_chat_rooms_responder_opened (alert_responder_residence_id, opened_at)
);

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    sender_residence_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_room FOREIGN KEY (chat_room_id)
        REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_residence_id)
        REFERENCES residences (id),
    INDEX idx_chat_messages_room_id (chat_room_id, id)
);
