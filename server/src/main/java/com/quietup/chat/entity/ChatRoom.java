package com.quietup.chat.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.quietup.noise.entity.NoiseAlert;
import com.quietup.residence.entity.Residence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_rooms_noise_alert_id",
                columnNames = "noise_alert_id"),
        indexes = {
                @Index(
                        name = "idx_chat_rooms_sender_opened",
                        columnList = "alert_sender_residence_id, opened_at"),
                @Index(
                        name = "idx_chat_rooms_responder_opened",
                        columnList = "alert_responder_residence_id, opened_at")
        })
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "noise_alert_id", nullable = false, unique = true)
    private NoiseAlert noiseAlert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_sender_residence_id", nullable = false)
    private Residence alertSenderResidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_responder_residence_id", nullable = false)
    private Residence alertResponderResidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ChatRoom() {
    }

    public ChatRoom(
            NoiseAlert noiseAlert,
            Residence alertSenderResidence,
            Residence alertResponderResidence) {
        if (alertSenderResidence.getId().equals(alertResponderResidence.getId())) {
            throw new IllegalArgumentException("채팅 참여자는 서로 달라야 합니다.");
        }
        this.noiseAlert = noiseAlert;
        this.alertSenderResidence = alertSenderResidence;
        this.alertResponderResidence = alertResponderResidence;
        this.status = ChatRoomStatus.OPEN;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.openedAt = now;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public NoiseAlert getNoiseAlert() {
        return noiseAlert;
    }

    public ChatRoomStatus getStatus() {
        return status;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public boolean isParticipant(Long residenceId) {
        return isAlertSender(residenceId) || alertResponderResidence.getId().equals(residenceId);
    }

    public boolean isAlertSender(Long residenceId) {
        return alertSenderResidence.getId().equals(residenceId);
    }
}
