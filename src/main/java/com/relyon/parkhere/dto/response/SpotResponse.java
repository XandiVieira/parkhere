package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.enums.SpotType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SpotResponse(
        UUID id,
        String name,
        SpotType type,
        double latitude,
        double longitude,
        double priceMin,
        double priceMax,
        double trustScore,
        int totalConfirmations,
        UUID createdBy,
        LocalDateTime createdAt
) {
    public static SpotResponse from(ParkingSpot spot) {
        return new SpotResponse(
                spot.getId(),
                spot.getName(),
                spot.getType(),
                spot.getLocation().getY(),
                spot.getLocation().getX(),
                spot.getPriceMin(),
                spot.getPriceMax(),
                spot.getTrustScore(),
                spot.getTotalConfirmations(),
                spot.getCreatedBy().getId(),
                spot.getCreatedAt()
        );
    }
}
