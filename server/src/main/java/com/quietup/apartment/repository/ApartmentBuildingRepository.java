package com.quietup.apartment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quietup.apartment.entity.ApartmentBuilding;

public interface ApartmentBuildingRepository extends JpaRepository<ApartmentBuilding, Long> {

    List<ApartmentBuilding> findByApartmentComplexIdOrderByBuildingNumberAscIdAsc(Long apartmentComplexId);
}
