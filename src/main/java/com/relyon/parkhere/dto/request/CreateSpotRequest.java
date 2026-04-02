package com.relyon.parkhere.dto.request;

import com.relyon.parkhere.model.enums.SpotType;
import jakarta.validation.constraints.*;

public record CreateSpotRequest(
        @NotBlank String name,
        @NotNull SpotType type,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @PositiveOrZero double priceMin,
        @PositiveOrZero double priceMax
) {}
