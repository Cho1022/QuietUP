package com.quietup.apartment.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.apartment.dto.ApartmentBuildingResponse;
import com.quietup.apartment.dto.ApartmentSearchResponse;
import com.quietup.apartment.repository.ApartmentBuildingRepository;
import com.quietup.apartment.repository.ApartmentComplexRepository;
import com.quietup.global.error.ApartmentNotFoundException;
import com.quietup.global.error.InvalidApartmentSearchQueryException;

@Service
public class ApartmentQueryService {

    private static final int SEARCH_LIMIT = 20;

    private final ApartmentComplexRepository apartmentComplexRepository;
    private final ApartmentBuildingRepository apartmentBuildingRepository;

    public ApartmentQueryService(
            ApartmentComplexRepository apartmentComplexRepository,
            ApartmentBuildingRepository apartmentBuildingRepository) {
        this.apartmentComplexRepository = apartmentComplexRepository;
        this.apartmentBuildingRepository = apartmentBuildingRepository;
    }

    @Transactional(readOnly = true)
    public List<ApartmentSearchResponse> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            throw new InvalidApartmentSearchQueryException();
        }

        PageRequest pageRequest = PageRequest.of(
                0,
                SEARCH_LIMIT,
                Sort.by("name").ascending()
                        .and(Sort.by("roadAddress").ascending())
                        .and(Sort.by("id").ascending()));

        return apartmentComplexRepository.search(normalizedQuery, pageRequest).stream()
                .map(apartment -> new ApartmentSearchResponse(
                        apartment.getId(),
                        apartment.getName(),
                        apartment.getRoadAddress()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApartmentBuildingResponse> getBuildings(Long apartmentId) {
        if (!apartmentComplexRepository.existsById(apartmentId)) {
            throw new ApartmentNotFoundException();
        }

        return apartmentBuildingRepository
                .findByApartmentComplexIdOrderByBuildingNumberAscIdAsc(apartmentId).stream()
                .map(building -> new ApartmentBuildingResponse(
                        building.getId(),
                        building.getBuildingNumber()))
                .toList();
    }
}
