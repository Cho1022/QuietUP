package com.quietup.residence.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.apartment.entity.ApartmentBuilding;
import com.quietup.apartment.entity.ApartmentComplex;
import com.quietup.apartment.entity.ApartmentUnit;
import com.quietup.apartment.repository.ApartmentUnitRepository;
import com.quietup.global.error.InvalidResidenceVerificationException;
import com.quietup.global.error.ResidenceAlreadyVerifiedException;
import com.quietup.global.error.UserNotFoundException;
import com.quietup.residence.dto.ResidenceStatusResponse;
import com.quietup.residence.dto.ResidenceVerificationRequest;
import com.quietup.residence.dto.ResidenceVerificationResponse;
import com.quietup.residence.entity.Residence;
import com.quietup.residence.entity.ResidenceVerificationCode;
import com.quietup.residence.repository.ResidenceRepository;
import com.quietup.residence.repository.ResidenceVerificationCodeRepository;
import com.quietup.user.entity.User;
import com.quietup.user.repository.UserRepository;

@Service
public class ResidenceVerificationService {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("[A-Z0-9-]{8,32}");
    private static final String RESIDENCE_USER_UNIQUE_CONSTRAINT = "uk_residences_user_id";

    private final UserRepository userRepository;
    private final ApartmentUnitRepository apartmentUnitRepository;
    private final ResidenceVerificationCodeRepository verificationCodeRepository;
    private final ResidenceRepository residenceRepository;

    public ResidenceVerificationService(
            UserRepository userRepository,
            ApartmentUnitRepository apartmentUnitRepository,
            ResidenceVerificationCodeRepository verificationCodeRepository,
            ResidenceRepository residenceRepository) {
        this.userRepository = userRepository;
        this.apartmentUnitRepository = apartmentUnitRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.residenceRepository = residenceRepository;
    }

    @Transactional
    public ResidenceVerificationResponse verify(String subject, ResidenceVerificationRequest request) {
        User user = findUser(subject);
        if (residenceRepository.existsByUserId(user.getId())) {
            throw new ResidenceAlreadyVerifiedException();
        }

        ApartmentUnit unit = apartmentUnitRepository.findForVerification(
                        request.apartmentId(),
                        request.buildingNumber(),
                        request.unitNumber())
                .orElseThrow(InvalidResidenceVerificationException::new);
        String codeHash = hashNormalizedCode(request.verificationCode());
        ResidenceVerificationCode verificationCode = verificationCodeRepository
                .findByCodeHashForUpdate(codeHash)
                .orElseThrow(InvalidResidenceVerificationException::new);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (!verificationCode.isFor(unit)
                || verificationCode.isExpired(now)
                || verificationCode.isUsed()) {
            throw new InvalidResidenceVerificationException();
        }

        verificationCode.markUsed(user, now);
        verificationCodeRepository.flush();

        Residence residence;
        try {
            residence = residenceRepository.saveAndFlush(new Residence(user, unit, now));
        } catch (DataIntegrityViolationException exception) {
            if (isResidenceUserUniqueViolation(exception)) {
                throw new ResidenceAlreadyVerifiedException();
            }
            throw exception;
        }

        ApartmentBuilding building = residence.getUnit().getBuilding();
        return new ResidenceVerificationResponse(
                "VERIFIED",
                building.getApartmentComplex().getName(),
                building.getBuildingNumber(),
                residence.getUnit().getUnitNumber(),
                residence.getVerifiedAt());
    }

    @Transactional(readOnly = true)
    public ResidenceStatusResponse getMyStatus(String subject) {
        User user = findUser(subject);
        return residenceRepository.findByUserId(user.getId())
                .map(this::toStatusResponse)
                .orElseGet(ResidenceStatusResponse::unverified);
    }

    private User findUser(String subject) {
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new UserNotFoundException();
        }
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private String hashNormalizedCode(String rawCode) {
        if (rawCode == null) {
            throw new InvalidResidenceVerificationException();
        }

        String normalizedCode = rawCode.trim().toUpperCase(Locale.ROOT);
        if (!VERIFICATION_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new InvalidResidenceVerificationException();
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private boolean isResidenceUserUniqueViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName == null) {
                    continue;
                }
                int qualifierSeparator = constraintName.lastIndexOf('.');
                String unqualifiedConstraintName = constraintName.substring(qualifierSeparator + 1);
                if (RESIDENCE_USER_UNIQUE_CONSTRAINT.equals(unqualifiedConstraintName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ResidenceStatusResponse toStatusResponse(Residence residence) {
        ApartmentUnit unit = residence.getUnit();
        ApartmentBuilding building = unit.getBuilding();
        ApartmentComplex apartment = building.getApartmentComplex();
        return ResidenceStatusResponse.verified(
                apartment.getName(),
                apartment.getRoadAddress(),
                building.getBuildingNumber(),
                unit.getUnitNumber(),
                residence.getVerifiedAt());
    }
}
