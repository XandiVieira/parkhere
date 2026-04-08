package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.CreateSpotRequest;
import com.relyon.parkhere.dto.request.UpdateSpotRequest;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.service.FavoriteService;
import com.relyon.parkhere.service.ParkingSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Parking Spots", description = "Spot CRUD, search, and favorites")
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;
    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<SpotResponse> create(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody CreateSpotRequest request,
                                               @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSpotService.create(request, user, force));
    }

    @GetMapping
    public ResponseEntity<Page<SpotResponse>> search(@RequestParam double lat,
                                                      @RequestParam double lng,
                                                      @RequestParam(defaultValue = "800") double radius,
                                                      @RequestParam(required = false) SpotType type,
                                                      @RequestParam(required = false) Double maxPrice,
                                                      @RequestParam(required = false) Boolean requiresBooking,
                                                      @RequestParam(required = false) Double minTrustScore,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(parkingSpotService.searchWithFilters(lat, lng, radius, type, maxPrice,
                requiresBooking, minTrustScore, query, PageRequest.of(page, size)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpotResponse> update(@AuthenticationPrincipal User user,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody UpdateSpotRequest request) {
        return ResponseEntity.ok(parkingSpotService.update(id, request, user));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        favoriteService.addFavorite(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        favoriteService.removeFavorite(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpotResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(parkingSpotService.getById(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<SpotResponse>> getMySpots(@AuthenticationPrincipal User user,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(parkingSpotService.getByUser(user.getId(), PageRequest.of(page, size)));
    }
}
