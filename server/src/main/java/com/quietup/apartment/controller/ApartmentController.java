package com.quietup.apartment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quietup.apartment.dto.ApartmentBuildingResponse;
import com.quietup.apartment.dto.ApartmentSearchResponse;
import com.quietup.apartment.service.ApartmentQueryService;

@RestController
@RequestMapping("/api/v1/apartments")
public class ApartmentController {

    private final ApartmentQueryService apartmentQueryService;

    public ApartmentController(ApartmentQueryService apartmentQueryService) {
        this.apartmentQueryService = apartmentQueryService;
    }

    @GetMapping
    public List<ApartmentSearchResponse> search(@RequestParam String query) {
        return apartmentQueryService.search(query);
    }

    @GetMapping("/{apartmentId}/buildings")
    public List<ApartmentBuildingResponse> getBuildings(@PathVariable Long apartmentId) {
        return apartmentQueryService.getBuildings(apartmentId);
    }
}
