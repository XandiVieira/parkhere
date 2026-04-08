package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.UpdatePreferencesRequest;
import com.relyon.parkhere.dto.response.PreferencesResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.UserPreference;
import com.relyon.parkhere.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(UUID userId) {
        return userPreferenceRepository.findByUserId(userId)
                .map(pref -> {
                    log.debug("Found preferences for user {}", userId);
                    return PreferencesResponse.from(pref);
                })
                .orElseGet(() -> {
                    log.debug("No preferences found for user {}, returning defaults", userId);
                    return PreferencesResponse.defaults();
                });
    }

    @Transactional
    public PreferencesResponse updatePreferences(User user, UpdatePreferencesRequest request) {
        var preference = userPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> UserPreference.builder()
                        .user(user)
                        .build());

        var spotTypes = request.defaultSpotTypes() != null && !request.defaultSpotTypes().isEmpty()
                ? String.join(",", request.defaultSpotTypes()) : null;
        var trustLevels = request.defaultTrustLevels() != null && !request.defaultTrustLevels().isEmpty()
                ? String.join(",", request.defaultTrustLevels()) : null;

        preference.setDefaultSpotTypes(spotTypes);
        preference.setDefaultTrustLevels(trustLevels);
        preference.setFreeOnly(request.freeOnly());

        var saved = userPreferenceRepository.save(preference);
        log.info("Preferences updated for user {}", user.getEmail());
        return PreferencesResponse.from(saved);
    }
}
