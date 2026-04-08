package com.relyon.parkhere.dto.request;

import java.util.List;

public record UpdatePreferencesRequest(
        List<String> defaultSpotTypes,
        List<String> defaultTrustLevels,
        boolean freeOnly
) {}
