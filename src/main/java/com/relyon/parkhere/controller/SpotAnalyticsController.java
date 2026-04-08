package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.response.SpotAnalyticsResponse;
import com.relyon.parkhere.service.SpotAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spots/{spotId}")
@RequiredArgsConstructor
public class SpotAnalyticsController {

    private final SpotAnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<SpotAnalyticsResponse> getAnalytics(@PathVariable UUID spotId) {
        return ResponseEntity.ok(analyticsService.getAnalytics(spotId));
    }
}
