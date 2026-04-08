package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.enums.SpotType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ParkingSpotRepositoryCustom {

    Page<ParkingSpot> searchWithFilters(double lat, double lng, double radiusMeters,
                                        SpotType type, Double maxPrice,
                                        Boolean requiresBooking, Double minTrustScore,
                                        String query, Pageable pageable);
}
