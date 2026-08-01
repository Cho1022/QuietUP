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
        name = "apartment_units",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_apartment_units_building_number",
                        columnNames = {"building_id", "unit_number"}),
                @UniqueConstraint(
                        name = "uk_apartment_units_building_floor_line",
                        columnNames = {"building_id", "floor_number", "line_number"})
        })
public class ApartmentUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private ApartmentBuilding building;

    @Column(name = "unit_number", nullable = false, length = 20)
    private String unitNumber;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ApartmentUnit() {
    }

    public ApartmentUnit(
            ApartmentBuilding building,
            String unitNumber,
            int floorNumber,
            int lineNumber) {
        this.building = building;
        this.unitNumber = unitNumber;
        this.floorNumber = floorNumber;
        this.lineNumber = lineNumber;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public ApartmentBuilding getBuilding() {
        return building;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
