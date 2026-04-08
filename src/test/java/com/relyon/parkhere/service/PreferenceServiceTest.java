package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.UpdatePreferencesRequest;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.UserPreference;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.UserPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @InjectMocks
    private PreferenceService preferenceService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void getPreferences_shouldReturnDefaultsWhenNoneExist() {
        var userId = UUID.randomUUID();
        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var result = preferenceService.getPreferences(userId);

        assertTrue(result.defaultSpotTypes().isEmpty());
        assertTrue(result.defaultTrustLevels().isEmpty());
        assertFalse(result.freeOnly());
    }

    @Test
    void getPreferences_shouldReturnExistingPreferences() {
        var user = buildUser();
        var pref = UserPreference.builder()
                .user(user)
                .defaultSpotTypes("STREET,PARKING_LOT")
                .defaultTrustLevels("HIGH,MEDIUM")
                .freeOnly(true)
                .build();
        pref.setCreatedAt(LocalDateTime.now());
        pref.setUpdatedAt(LocalDateTime.now());
        when(userPreferenceRepository.findByUserId(user.getId())).thenReturn(Optional.of(pref));

        var result = preferenceService.getPreferences(user.getId());

        assertEquals(List.of("STREET", "PARKING_LOT"), result.defaultSpotTypes());
        assertEquals(List.of("HIGH", "MEDIUM"), result.defaultTrustLevels());
        assertTrue(result.freeOnly());
    }

    @Test
    void updatePreferences_shouldUpsertAndReturnUpdated() {
        var user = buildUser();
        var request = new UpdatePreferencesRequest(
                List.of("STREET", "MALL"), List.of("HIGH"), true
        );
        when(userPreferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> {
            var saved = inv.<UserPreference>getArgument(0);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        var result = preferenceService.updatePreferences(user, request);

        assertEquals(List.of("STREET", "MALL"), result.defaultSpotTypes());
        assertEquals(List.of("HIGH"), result.defaultTrustLevels());
        assertTrue(result.freeOnly());
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }
}
