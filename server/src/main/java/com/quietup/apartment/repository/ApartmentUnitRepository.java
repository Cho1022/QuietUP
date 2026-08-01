package com.quietup.apartment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quietup.apartment.entity.ApartmentUnit;

public interface ApartmentUnitRepository extends JpaRepository<ApartmentUnit, Long> {

    @Query("""
            select apartmentUnit
            from ApartmentUnit apartmentUnit
            join fetch apartmentUnit.building building
            join fetch building.apartmentComplex apartment
            where apartment.id = :apartmentId
              and building.buildingNumber = :buildingNumber
              and apartmentUnit.unitNumber = :unitNumber
            """)
    Optional<ApartmentUnit> findForVerification(
            @Param("apartmentId") Long apartmentId,
            @Param("buildingNumber") String buildingNumber,
            @Param("unitNumber") String unitNumber);
}
