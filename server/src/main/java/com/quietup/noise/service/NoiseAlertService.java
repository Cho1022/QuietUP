package com.quietup.noise.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.apartment.entity.ApartmentBuilding;
import com.quietup.apartment.entity.ApartmentComplex;
import com.quietup.apartment.entity.ApartmentUnit;
import com.quietup.apartment.repository.ApartmentUnitRepository;
import com.quietup.global.error.NoiseAlertNotFoundException;
import com.quietup.global.error.NoiseAlertTargetUnavailableException;
import com.quietup.global.error.ResidenceRequiredException;
import com.quietup.noise.dto.CreateNoiseAlertRequest;
import com.quietup.noise.dto.CreatedNoiseAlertResponse;
import com.quietup.noise.dto.NoiseAlertDetailResponse;
import com.quietup.noise.dto.ReceivedNoiseAlertResponse;
import com.quietup.noise.dto.SentNoiseAlertResponse;
import com.quietup.noise.entity.NoiseAlert;
import com.quietup.noise.repository.NoiseAlertRepository;
import com.quietup.residence.entity.Residence;
import com.quietup.residence.repository.ResidenceRepository;

@Service
public class NoiseAlertService {

    private static final String RECEIVED_SENDER_LABEL = "알림을 보낸 이웃";

    private final ResidenceRepository residenceRepository;
    private final ApartmentUnitRepository apartmentUnitRepository;
    private final NoiseAlertRepository noiseAlertRepository;

    public NoiseAlertService(
            ResidenceRepository residenceRepository,
            ApartmentUnitRepository apartmentUnitRepository,
            NoiseAlertRepository noiseAlertRepository) {
        this.residenceRepository = residenceRepository;
        this.apartmentUnitRepository = apartmentUnitRepository;
        this.noiseAlertRepository = noiseAlertRepository;
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
                alert.getCreatedAt(),
                alert.getRespondedAt(),
                alert.getResolvedAt());
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
                alert.getCreatedAt(),
                alert.getRespondedAt(),
                alert.getResolvedAt());
    }
}
