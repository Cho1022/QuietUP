package com.quietup.noise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quietup.noise.entity.NoiseAlert;

public interface NoiseAlertRepository extends JpaRepository<NoiseAlert, Long> {

    List<NoiseAlert> findBySenderResidenceIdOrderByCreatedAtDescIdDesc(Long senderResidenceId);

    List<NoiseAlert> findByTargetUnitIdOrderByCreatedAtDescIdDesc(Long targetUnitId);
}
