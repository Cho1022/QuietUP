package com.quietup.apartment.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quietup.apartment.entity.ApartmentComplex;

public interface ApartmentComplexRepository extends JpaRepository<ApartmentComplex, Long> {

    @Query("""
            select apartment
            from ApartmentComplex apartment
            where lower(apartment.name) like lower(concat('%', :query, '%'))
               or lower(apartment.roadAddress) like lower(concat('%', :query, '%'))
            """)
    List<ApartmentComplex> search(@Param("query") String query, Pageable pageable);
}
