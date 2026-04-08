package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.UserPreference;

import java.util.List;

public record PreferencesResponse(
        List<String> defaultSpotTypes,
        List<String> defaultTrustLevels,
        boolean freeOnly
) {
    public static PreferencesResponse from(UserPreference pref) {
        return new PreferencesResponse(
                pref.getDefaultSpotTypes() != null ? List.of(pref.getDefaultSpotTypes().split(",")) : List.of(),
                pref.getDefaultTrustLevels() != null ? List.of(pref.getDefaultTrustLevels().split(",")) : List.of(),
                pref.isFreeOnly()
        );
    }

    public static PreferencesResponse defaults() {
        return new PreferencesResponse(List.of(), List.of(), false);
    }
}
