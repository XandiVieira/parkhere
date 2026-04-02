package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateSpotRequest;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.exception.NearbySpotExistsException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.ParkingSpotSchedule;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
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
        log.info("Parking spot created: {} by user {}{}", saved.getId(), user.getEmail(), force ? " (forced)" : "");
        return SpotResponse.from(saved);
    }

    public List<SpotResponse> searchByRadius(double lat, double lng, double radiusMeters) {
        log.debug("Searching spots within {}m of ({}, {})", radiusMeters, lat, lng);
        return parkingSpotRepository.findWithinRadius(lat, lng, radiusMeters).stream()
                .map(SpotResponse::from)
                .toList();
    }

    public SpotResponse getById(UUID id) {
        var spot = parkingSpotRepository.findById(id)
                .orElseThrow(() -> new SpotNotFoundException(id.toString()));
        return SpotResponse.from(spot);
    }

    public List<SpotResponse> getByUser(UUID userId) {
        return parkingSpotRepository.findByCreatedById(userId).stream()
                .map(SpotResponse::from)
                .toList();
    }
}
