package com.quietup.residence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quietup.residence.entity.ResidenceVerificationCode;

import jakarta.persistence.LockModeType;

public interface ResidenceVerificationCodeRepository extends JpaRepository<ResidenceVerificationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select verificationCode
            from ResidenceVerificationCode verificationCode
            join fetch verificationCode.unit
            where verificationCode.codeHash = :codeHash
            """)
    Optional<ResidenceVerificationCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
