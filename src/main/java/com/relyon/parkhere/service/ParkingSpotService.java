package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateSpotRequest;
import com.relyon.parkhere.dto.request.UpdateSpotRequest;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.exception.NearbySpotExistsException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.exception.UnauthorizedSpotModificationException;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.ParkingSpotSchedule;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final List<GeocodingService> geocodingServices;
    private final GamificationService gamificationService;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double NEARBY_THRESHOLD_METERS = 50.0;

    @Transactional
    public SpotResponse create(CreateSpotRequest request, User user, boolean force) {
        if (!force) {
            var nearbySpots = searchByRadius(request.latitude(), request.longitude(), NEARBY_THRESHOLD_METERS);
            if (!nearbySpots.isEmpty()) {
                log.info("Spot creation near ({}, {}) blocked — {} nearby spots found",
                        request.latitude(), request.longitude(), nearbySpots.size());
                throw new NearbySpotExistsException(nearbySpots);
            }
        }

        var point = GEOMETRY_FACTORY.createPoint(new Coordinate(request.longitude(), request.latitude()));

        var spot = ParkingSpot.builder()
                .name(request.name())
                .type(request.type())
                .location(point)
                .priceMin(request.priceMin())
                .priceMax(request.priceMax())
                .requiresBooking(request.requiresBooking())
                .estimatedSpots(request.estimatedSpots())
                .informalChargeFrequency(request.informalChargeFrequency() != null ? request.informalChargeFrequency() : "UNKNOWN")
                .notes(request.notes())
                .createdBy(user)
                .build();

        if (request.schedules() != null) {
            request.schedules().forEach(s -> {
                var schedule = ParkingSpotSchedule.builder()
                        .parkingSpot(spot)
                        .dayOfWeek(s.dayOfWeek())
                        .openTime(s.openTime())
                        .closeTime(s.closeTime())
                        .paidOnly(s.paidOnly())
                        .build();
                spot.getSchedules().add(schedule);
            });
        }

        var saved = parkingSpotRepository.save(spot);

        if (!geocodingServices.isEmpty()) {
            var address = geocodingServices.getFirst().reverseGeocode(request.latitude(), request.longitude());
            if (address != null) {
                saved.setAddress(address);
                parkingSpotRepository.save(saved);
            }
        }

        gamificationService.awardPointsForSpotCreation(user);

        log.info("Parking spot created: {} by user {}{}", saved.getId(), user.getEmail(), force ? " (forced)" : "");
        return SpotResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SpotResponse> searchByRadius(double lat, double lng, double radiusMeters) {
        log.debug("Searching spots within {}m of ({}, {})", radiusMeters, lat, lng);
        return parkingSpotRepository.findWithinRadius(lat, lng, radiusMeters).stream()
                .map(SpotResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SpotResponse> searchWithFilters(double lat, double lng, double radiusMeters,
                                                 SpotType type, Double maxPrice,
                                                 Boolean requiresBooking, Double minTrustScore,
                                                 String query, Pageable pageable) {
        log.debug("Searching spots within {}m of ({}, {}) with filters", radiusMeters, lat, lng);
        return parkingSpotRepository.searchWithFilters(lat, lng, radiusMeters, type, maxPrice,
                requiresBooking, minTrustScore, query, pageable).map(SpotResponse::from);
    }

    @Transactional(readOnly = true)
    public SpotResponse getById(UUID id) {
        var spot = parkingSpotRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new SpotNotFoundException(id.toString()));
        return SpotResponse.from(spot);
    }

    @Transactional
    public SpotResponse update(UUID id, UpdateSpotRequest request, User user) {
        var spot = parkingSpotRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new SpotNotFoundException(id.toString()));

        if (!spot.getCreatedBy().getId().equals(user.getId())) {
            throw new UnauthorizedSpotModificationException();
        }

        spot.setName(request.name());
        spot.setPriceMin(request.priceMin());
        spot.setPriceMax(request.priceMax());
        spot.setRequiresBooking(request.requiresBooking());
        spot.setEstimatedSpots(request.estimatedSpots());
        if (request.informalChargeFrequency() != null) spot.setInformalChargeFrequency(request.informalChargeFrequency());
        spot.setNotes(request.notes());

        spot.getSchedules().clear();
        if (request.schedules() != null) {
            request.schedules().forEach(s -> {
                var schedule = ParkingSpotSchedule.builder()
                        .parkingSpot(spot)
                        .dayOfWeek(s.dayOfWeek())
                        .openTime(s.openTime())
                        .closeTime(s.closeTime())
                        .paidOnly(s.paidOnly())
                        .build();
                spot.getSchedules().add(schedule);
            });
        }

        var saved = parkingSpotRepository.save(spot);
        log.info("Parking spot updated: {} by user {}", saved.getId(), user.getEmail());
        return SpotResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<SpotResponse> getByUser(UUID userId, Pageable pageable) {
        return parkingSpotRepository.findByCreatedByIdAndActiveTrue(userId, pageable)
                .map(SpotResponse::from);
    }
}
