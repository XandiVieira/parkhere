package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.CreateSpotRequest;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.service.ParkingSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;

    @PostMapping
    public ResponseEntity<SpotResponse> create(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody CreateSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSpotService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<List<SpotResponse>> search(@RequestParam double lat,
                                                     @RequestParam double lng,
                                                     @RequestParam(defaultValue = "800") double radius) {
        return ResponseEntity.ok(parkingSpotService.searchByRadius(lat, lng, radius));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpotResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(parkingSpotService.getById(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<SpotResponse>> getMySpots(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(parkingSpotService.getByUser(user.getId()));
    }
}
