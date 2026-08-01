package com.quietup.apartment.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "apartment_buildings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_apartment_buildings_complex_number",
                columnNames = {"apartment_complex_id", "building_number"}))
public class ApartmentBuilding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_complex_id", nullable = false)
    private ApartmentComplex apartmentComplex;

    @Column(name = "building_number", nullable = false, length = 20)
    private String buildingNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ApartmentBuilding() {
    }

    public ApartmentBuilding(ApartmentComplex apartmentComplex, String buildingNumber) {
        this.apartmentComplex = apartmentComplex;
        this.buildingNumber = buildingNumber;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public ApartmentComplex getApartmentComplex() {
        return apartmentComplex;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }
}
