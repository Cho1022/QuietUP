package com.quietup.residence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quietup.residence.entity.Residence;

public interface ResidenceRepository extends JpaRepository<Residence, Long> {

    boolean existsByUserId(Long userId);

    boolean existsByUnitId(Long unitId);

    @Query("""
            select residence
            from Residence residence
            join fetch residence.unit apartmentUnit
            join fetch apartmentUnit.building building
            join fetch building.apartmentComplex
            where residence.user.id = :userId
            """)
    Optional<Residence> findByUserId(@Param("userId") Long userId);
}
