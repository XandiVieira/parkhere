package com.relyon.parkhere.dto.response;

import java.util.List;

public record NearbySpotConflictResponse(
        String message,
        List<SpotResponse> nearbySpots
) {}
