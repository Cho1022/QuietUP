package com.quietup.noise.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.quietup.apartment.entity.ApartmentComplex;
import com.quietup.apartment.entity.ApartmentUnit;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "noise_alerts",
        indexes = {
                @Index(
                        name = "idx_noise_alerts_sender_created",
                        columnList = "sender_residence_id, created_at"),
                @Index(
                        name = "idx_noise_alerts_target_created",
                        columnList = "target_unit_id, created_at"),
                @Index(
                        name = "idx_noise_alerts_apartment_created",
                        columnList = "apartment_complex_id, created_at")
        })
public class NoiseAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_complex_id", nullable = false)
    private ApartmentComplex apartmentComplex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_residence_id", nullable = false)
    private Residence senderResidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_unit_id", nullable = false)
    private ApartmentUnit targetUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoiseDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "noise_type", nullable = false, length = 30)
    private NoiseType noiseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoiseAlertStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected NoiseAlert() {
    }

    public NoiseAlert(
            ApartmentComplex apartmentComplex,
            Residence senderResidence,
            ApartmentUnit targetUnit,
            NoiseDirection direction,
            NoiseType noiseType) {
        this.apartmentComplex = apartmentComplex;
        this.senderResidence = senderResidence;
        this.targetUnit = targetUnit;
        this.direction = direction;
        this.noiseType = noiseType;
        this.status = NoiseAlertStatus.SENT;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Residence getSenderResidence() {
        return senderResidence;
    }

    public ApartmentUnit getTargetUnit() {
        return targetUnit;
    }

    public NoiseDirection getDirection() {
        return direction;
    }

    public NoiseType getNoiseType() {
        return noiseType;
    }

    public NoiseAlertStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public boolean isResponded() {
        return status == NoiseAlertStatus.RESPONDED;
    }

    public boolean isResolved() {
        return status == NoiseAlertStatus.RESOLVED;
    }

    public void markResponded(LocalDateTime now) {
        this.status = NoiseAlertStatus.RESPONDED;
        this.respondedAt = now;
    }

    public void resolve(LocalDateTime now) {
        if (!isResolved()) {
            this.status = NoiseAlertStatus.RESOLVED;
            this.resolvedAt = now;
        }
    }
}
