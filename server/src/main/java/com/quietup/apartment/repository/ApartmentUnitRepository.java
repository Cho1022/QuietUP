package com.quietup.apartment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quietup.apartment.entity.ApartmentUnit;

public interface ApartmentUnitRepository extends JpaRepository<ApartmentUnit, Long> {
}
