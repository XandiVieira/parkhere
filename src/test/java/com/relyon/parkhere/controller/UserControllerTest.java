package com.relyon.parkhere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.request.UpdatePreferencesRequest;
import com.relyon.parkhere.dto.request.UpdateUserRequest;
import com.relyon.parkhere.dto.response.PreferencesResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.FavoriteService;
import com.relyon.parkhere.service.GamificationService;
import com.relyon.parkhere.service.PreferenceService;
import com.relyon.parkhere.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private GamificationService gamificationService;

    @MockitoBean
    private PreferenceService preferenceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LocalizedMessageService localizedMessageService;

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
    void getProfile_shouldReturn200() throws Exception {
        var user = buildUser();
        var response = new UserResponse(user.getId(), "John", null, "john@test.com", Role.USER, 0.0, null, user.getCreatedAt());
        when(userService.getProfile(any(User.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void getProfile_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_shouldReturn200() throws Exception {
        var user = buildUser();
        var request = new UpdateUserRequest("New Name", null);
        var response = new UserResponse(user.getId(), "New Name", null, "john@test.com", Role.USER, 0.0, null, user.getCreatedAt());
        when(userService.updateProfile(any(User.class), any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void updateProfile_shouldReturn400ForBlankName() throws Exception {
        var user = buildUser();
        var request = new UpdateUserRequest("", null);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPreferences_shouldReturn200() throws Exception {
        var user = buildUser();
        var response = new PreferencesResponse(List.of("STREET"), List.of("HIGH"), false);
        when(preferenceService.getPreferences(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me/preferences")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultSpotTypes[0]").value("STREET"))
                .andExpect(jsonPath("$.defaultTrustLevels[0]").value("HIGH"))
                .andExpect(jsonPath("$.freeOnly").value(false));
    }

    @Test
    void updatePreferences_shouldReturn200() throws Exception {
        var user = buildUser();
        var request = new UpdatePreferencesRequest(List.of("STREET", "MALL"), List.of("HIGH"), true);
        var response = new PreferencesResponse(List.of("STREET", "MALL"), List.of("HIGH"), true);
        when(preferenceService.updatePreferences(any(User.class), any(UpdatePreferencesRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultSpotTypes[0]").value("STREET"))
                .andExpect(jsonPath("$.defaultSpotTypes[1]").value("MALL"))
                .andExpect(jsonPath("$.freeOnly").value(true));
    }
}
