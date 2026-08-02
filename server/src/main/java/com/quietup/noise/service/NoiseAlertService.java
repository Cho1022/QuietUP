package com.quietup.noise.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.apartment.entity.ApartmentBuilding;
import com.quietup.apartment.entity.ApartmentComplex;
import com.quietup.apartment.entity.ApartmentUnit;
import com.quietup.apartment.repository.ApartmentUnitRepository;
import com.quietup.global.error.NoiseAlertAlreadyRespondedException;
import com.quietup.global.error.NoiseAlertAlreadyResolvedException;
import com.quietup.global.error.NoiseAlertNotFoundException;
import com.quietup.global.error.NoiseAlertResolveNotAllowedException;
import com.quietup.global.error.NoiseAlertResponseNotAllowedException;
import com.quietup.global.error.NoiseAlertTargetUnavailableException;
import com.quietup.global.error.ResidenceRequiredException;
import com.quietup.noise.dto.CreateNoiseAlertRequest;
import com.quietup.noise.dto.CreatedNoiseAlertResponse;
import com.quietup.noise.dto.NoiseAlertDetailResponse;
import com.quietup.noise.dto.NoiseAlertResponseRequest;
import com.quietup.noise.dto.NoiseAlertResponseResult;
import com.quietup.noise.dto.ReceivedNoiseAlertResponse;
import com.quietup.noise.dto.SentNoiseAlertResponse;
import com.quietup.noise.entity.NoiseAlert;
import com.quietup.noise.entity.NoiseAlertResponse;
import com.quietup.noise.entity.ResponseType;
import com.quietup.noise.repository.NoiseAlertRepository;
import com.quietup.noise.repository.NoiseAlertResponseRepository;
import com.quietup.residence.entity.Residence;
import com.quietup.residence.repository.ResidenceRepository;

@Service
public class NoiseAlertService {

    private static final String RECEIVED_SENDER_LABEL = "알림을 보낸 이웃";
    private static final String RESPONSE_UNIQUE_CONSTRAINT = "uk_noise_alert_responses_alert_id";

    private final ResidenceRepository residenceRepository;
    private final ApartmentUnitRepository apartmentUnitRepository;
    private final NoiseAlertRepository noiseAlertRepository;
    private final NoiseAlertResponseRepository noiseAlertResponseRepository;

    public NoiseAlertService(
            ResidenceRepository residenceRepository,
            ApartmentUnitRepository apartmentUnitRepository,
            NoiseAlertRepository noiseAlertRepository,
            NoiseAlertResponseRepository noiseAlertResponseRepository) {
        this.residenceRepository = residenceRepository;
        this.apartmentUnitRepository = apartmentUnitRepository;
        this.noiseAlertRepository = noiseAlertRepository;
        this.noiseAlertResponseRepository = noiseAlertResponseRepository;
    }

    @Transactional
    public CreatedNoiseAlertResponse create(String subject, CreateNoiseAlertRequest request) {
        Residence senderResidence = findCurrentResidence(subject);
        ApartmentUnit senderUnit = senderResidence.getUnit();
        ApartmentBuilding senderBuilding = senderUnit.getBuilding();
        ApartmentComplex apartmentComplex = senderBuilding.getApartmentComplex();
        int targetFloor = request.direction().targetFloor(senderUnit.getFloorNumber());

        ApartmentUnit targetUnit = apartmentUnitRepository.findByLocation(
                        senderBuilding.getId(),
                        targetFloor,
                        senderUnit.getLineNumber())
                .filter(unit -> apartmentComplex.getId().equals(
                        unit.getBuilding().getApartmentComplex().getId()))
                .filter(unit -> residenceRepository.existsByUnitId(unit.getId()))
                .orElseThrow(NoiseAlertTargetUnavailableException::new);

        NoiseAlert alert = noiseAlertRepository.save(new NoiseAlert(
                apartmentComplex,
                senderResidence,
                targetUnit,
                request.direction(),
                request.noiseType()));

        return new CreatedNoiseAlertResponse(
                alert.getId(),
                alert.getDirection().targetLabel(),
                alert.getDirection(),
                alert.getNoiseType(),
                alert.getNoiseType().displayMessage(alert.getDirection()),
                alert.getStatus(),
                alert.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ReceivedNoiseAlertResponse> getReceived(String subject) {
        Residence residence = findCurrentResidence(subject);
        return noiseAlertRepository
                .findByTargetUnitIdOrderByCreatedAtDescIdDesc(residence.getUnit().getId()).stream()
                .map(this::toReceivedResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SentNoiseAlertResponse> getSent(String subject) {
        Residence residence = findCurrentResidence(subject);
        return noiseAlertRepository
                .findBySenderResidenceIdOrderByCreatedAtDescIdDesc(residence.getId()).stream()
                .map(this::toSentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoiseAlertDetailResponse getDetail(String subject, Long noiseAlertId) {
        Residence residence = findCurrentResidence(subject);
        NoiseAlert alert = noiseAlertRepository.findById(noiseAlertId)
                .orElseThrow(NoiseAlertNotFoundException::new);
        boolean sender = alert.getSenderResidence().getId().equals(residence.getId());
        boolean receiver = alert.getTargetUnit().getId().equals(residence.getUnit().getId());
        if (!sender && !receiver) {
            throw new NoiseAlertNotFoundException();
        }

        String counterpartLabel = sender ? alert.getDirection().targetLabel() : RECEIVED_SENDER_LABEL;
        return new NoiseAlertDetailResponse(
                alert.getId(),
                counterpartLabel,
                alert.getDirection(),
                alert.getNoiseType(),
                alert.getNoiseType().displayMessage(alert.getDirection()),
                alert.getStatus(),
                findResponseType(alert.getId()),
                alert.getCreatedAt(),
                alert.getRespondedAt(),
                alert.getResolvedAt());
    }

    @Transactional
    public NoiseAlertResponseResult respond(
            String subject,
            Long noiseAlertId,
            NoiseAlertResponseRequest request) {
        Residence responderResidence = findCurrentResidence(subject);
        NoiseAlert alert = noiseAlertRepository.findByIdForUpdate(noiseAlertId)
                .orElseThrow(NoiseAlertNotFoundException::new);
        boolean sender = alert.getSenderResidence().getId().equals(responderResidence.getId());
        boolean receiver = alert.getTargetUnit().getId().equals(responderResidence.getUnit().getId());

        if (!sender && !receiver) {
            throw new NoiseAlertNotFoundException();
        }
        if (sender) {
            throw new NoiseAlertResponseNotAllowedException();
        }
        if (alert.isResolved()) {
            throw new NoiseAlertAlreadyResolvedException();
        }
        if (alert.isResponded() || noiseAlertResponseRepository.existsByNoiseAlertId(alert.getId())) {
            throw new NoiseAlertAlreadyRespondedException();
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        try {
            noiseAlertResponseRepository.saveAndFlush(new NoiseAlertResponse(
                    alert,
                    responderResidence,
                    request.responseType(),
                    now));
        } catch (DataIntegrityViolationException exception) {
            if (isResponseUniqueViolation(exception)) {
                throw new NoiseAlertAlreadyRespondedException();
            }
            throw exception;
        }
        alert.markResponded(now);

        return new NoiseAlertResponseResult(
                alert.getId(),
                request.responseType(),
                alert.getStatus(),
                alert.getRespondedAt());
    }

    @Transactional
    public void resolve(String subject, Long noiseAlertId) {
        Residence residence = findCurrentResidence(subject);
        NoiseAlert alert = noiseAlertRepository.findByIdForUpdate(noiseAlertId)
                .orElseThrow(NoiseAlertNotFoundException::new);
        boolean sender = alert.getSenderResidence().getId().equals(residence.getId());
        boolean receiver = alert.getTargetUnit().getId().equals(residence.getUnit().getId());

        if (!sender && !receiver) {
            throw new NoiseAlertNotFoundException();
        }
        if (!sender) {
            throw new NoiseAlertResolveNotAllowedException();
        }
        alert.resolve(LocalDateTime.now(ZoneOffset.UTC));
    }

    private Residence findCurrentResidence(String subject) {
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new ResidenceRequiredException();
        }
        return residenceRepository.findByUserId(userId)
                .orElseThrow(ResidenceRequiredException::new);
    }

    private SentNoiseAlertResponse toSentResponse(NoiseAlert alert) {
        return new SentNoiseAlertResponse(
                alert.getId(),
                alert.getDirection().targetLabel(),
                alert.getDirection(),
                alert.getNoiseType(),
                alert.getNoiseType().displayMessage(alert.getDirection()),
                alert.getStatus(),
                findResponseType(alert.getId()),
                alert.getCreatedAt(),
                alert.getRespondedAt(),
                alert.getResolvedAt());
    }

    private ReceivedNoiseAlertResponse toReceivedResponse(NoiseAlert alert) {
        return new ReceivedNoiseAlertResponse(
                alert.getId(),
                RECEIVED_SENDER_LABEL,
                alert.getDirection(),
                alert.getNoiseType(),
                alert.getNoiseType().displayMessage(alert.getDirection()),
                alert.getStatus(),
                findResponseType(alert.getId()),
                alert.getCreatedAt(),
                alert.getRespondedAt(),
                alert.getResolvedAt());
    }

    private ResponseType findResponseType(Long noiseAlertId) {
        return noiseAlertResponseRepository.findByNoiseAlertId(noiseAlertId)
                .map(NoiseAlertResponse::getResponseType)
                .orElse(null);
    }

    private boolean isResponseUniqueViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName == null) {
                    continue;
                }
                int qualifierSeparator = constraintName.lastIndexOf('.');
                String unqualifiedConstraintName = constraintName.substring(qualifierSeparator + 1);
                if (RESPONSE_UNIQUE_CONSTRAINT.equals(unqualifiedConstraintName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
