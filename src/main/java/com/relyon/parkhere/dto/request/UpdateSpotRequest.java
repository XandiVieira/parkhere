package com.relyon.parkhere.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateSpotRequest(
        @NotBlank String name,
        @PositiveOrZero double priceMin,
        @PositiveOrZero double priceMax,
        boolean requiresBooking,
        @Positive Integer estimatedSpots,
        String informalChargeFrequency,
        @Size(max = 1000) String notes,
        List<@Valid ScheduleRequest> schedules
) {}
