package com.quietup.noise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quietup.noise.entity.NoiseAlertResponse;

public interface NoiseAlertResponseRepository extends JpaRepository<NoiseAlertResponse, Long> {

    boolean existsByNoiseAlertId(Long noiseAlertId);

    Optional<NoiseAlertResponse> findByNoiseAlertId(Long noiseAlertId);
}
