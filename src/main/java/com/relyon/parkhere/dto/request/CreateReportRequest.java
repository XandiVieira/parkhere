package com.relyon.parkhere.dto.request;

import com.relyon.parkhere.model.enums.AvailabilityStatus;
import jakarta.validation.constraints.*;

public record CreateReportRequest(
        @NotNull AvailabilityStatus availabilityStatus,
        @PositiveOrZero Double estimatedPrice,
        @Min(1) @Max(5) Integer safetyRating,
        boolean informalChargeReported,
        @Size(max = 500) String note,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double userLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double userLongitude
) {}
