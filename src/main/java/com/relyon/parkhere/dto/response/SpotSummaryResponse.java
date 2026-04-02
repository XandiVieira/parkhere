package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.SpotType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SpotSummaryResponse(
        UUID spotId,
        String name,
        SpotType type,
        double latitude,
        double longitude,
        double priceMin,
        double priceMax,
        boolean requiresBooking,
        double trustScore,
        int totalConfirmations,
        LocalDateTime lastConfirmedAt,
        AvailabilityStatus dominantAvailability,
        Double avgEstimatedPrice,
        Double avgSafetyRating,
        double informalChargePercentage,
        boolean informalChargeReportedRecently,
        LocalDateTime lastReportAt
) {}
