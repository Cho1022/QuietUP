package com.quietup.noise.entity;

import java.time.LocalDateTime;

import com.quietup.residence.entity.Residence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "noise_alert_responses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_noise_alert_responses_alert_id",
                columnNames = "noise_alert_id"))
public class NoiseAlertResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "noise_alert_id", nullable = false, unique = true)
    private NoiseAlert noiseAlert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responder_residence_id", nullable = false)
    private Residence responderResidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false, length = 30)
    private ResponseType responseType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NoiseAlertResponse() {
    }

    public NoiseAlertResponse(
            NoiseAlert noiseAlert,
            Residence responderResidence,
            ResponseType responseType,
            LocalDateTime createdAt) {
        this.noiseAlert = noiseAlert;
        this.responderResidence = responderResidence;
        this.responseType = responseType;
        this.createdAt = createdAt;
    }

    public ResponseType getResponseType() {
        return responseType;
    }
}
