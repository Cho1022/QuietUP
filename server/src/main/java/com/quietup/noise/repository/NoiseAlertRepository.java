package com.quietup.noise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quietup.noise.entity.NoiseAlert;

import jakarta.persistence.LockModeType;

public interface NoiseAlertRepository extends JpaRepository<NoiseAlert, Long> {

    List<NoiseAlert> findBySenderResidenceIdOrderByCreatedAtDescIdDesc(Long senderResidenceId);

    List<NoiseAlert> findByTargetUnitIdOrderByCreatedAtDescIdDesc(Long targetUnitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select alert from NoiseAlert alert where alert.id = :noiseAlertId")
    Optional<NoiseAlert> findByIdForUpdate(@Param("noiseAlertId") Long noiseAlertId);
}
