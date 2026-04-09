package com.relyon.parkhere.dto.request;

import com.relyon.parkhere.model.enums.SpotType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateSpotRequest(
        @NotBlank String name,
        @NotNull SpotType type,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @PositiveOrZero double priceMin,
        @PositiveOrZero double priceMax,
        boolean requiresBooking,
        @Positive Integer estimatedSpots,
        String informalChargeFrequency,
        @Size(max = 1000) String notes,
        List<@Valid ScheduleRequest> schedules
) {}
