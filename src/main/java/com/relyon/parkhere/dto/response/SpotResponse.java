package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.model.enums.TrustLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SpotResponse(
        UUID id,
        String name,
        SpotType type,
        double latitude,
        double longitude,
        double priceMin,
        double priceMax,
        boolean requiresBooking,
        Integer estimatedSpots,
        String notes,
        double trustScore,
        TrustLevel trustLevel,
        int totalConfirmations,
        LocalDateTime lastConfirmedAt,
        String address,
        String informalChargeFrequency,
        String coverImageUrl,
        List<ScheduleResponse> schedules,
        UUID createdBy,
        LocalDateTime createdAt
) {
    public static SpotResponse from(ParkingSpot spot) {
        var schedules = spot.getSchedules() != null
                ? spot.getSchedules().stream().map(ScheduleResponse::from).toList()
                : List.<ScheduleResponse>of();

        var coverUrl = spot.getCoverImage() != null
                ? "/api/v1/images/" + spot.getCoverImage()
                : null;

        return new SpotResponse(
                spot.getId(),
                spot.getName(),
                spot.getType(),
                spot.getLocation().getY(),
                spot.getLocation().getX(),
                spot.getPriceMin(),
                spot.getPriceMax(),
                spot.isRequiresBooking(),
                spot.getEstimatedSpots(),
                spot.getNotes(),
                spot.getTrustScore(),
                TrustLevel.fromScore(spot.getTrustScore()),
                spot.getTotalConfirmations(),
                spot.getLastConfirmedAt(),
                spot.getAddress(),
                spot.getInformalChargeFrequency(),
                coverUrl,
                schedules,
                spot.getCreatedBy().getId(),
                spot.getCreatedAt()
        );
    }
}
