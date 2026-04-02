package com.relyon.parkhere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.request.CreateSpotRequest;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.ParkingSpotService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSpotController.class)
@Import(SecurityConfig.class)
class ParkingSpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ParkingSpotService parkingSpotService;

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

    private SpotResponse sampleSpotResponse(UUID createdBy) {
        return new SpotResponse(
                UUID.randomUUID(), "Test Spot", SpotType.STREET,
                -22.9068, -43.1729, 5.0, 15.0, false, null,
                0.0, 0, null, List.of(),
                createdBy, LocalDateTime.now()
        );
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var user = buildUser();
        var request = new CreateSpotRequest("Test Spot", SpotType.STREET, -22.9068, -43.1729, 5.0, 15.0, false, null, null);
        var response = sampleSpotResponse(user.getId());
        when(parkingSpotService.create(any(CreateSpotRequest.class), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/spots")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Spot"))
                .andExpect(jsonPath("$.type").value("STREET"));
    }

    @Test
    void create_shouldReturn401WhenUnauthenticated() throws Exception {
        var request = new CreateSpotRequest("Test Spot", SpotType.STREET, -22.9068, -43.1729, 5.0, 15.0, false, null, null);

        mockMvc.perform(post("/api/v1/spots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturn400ForInvalidInput() throws Exception {
        var user = buildUser();
        var request = new CreateSpotRequest("", null, -22.9068, -43.1729, -1.0, 15.0, false, null, null);

        mockMvc.perform(post("/api/v1/spots")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_shouldReturn200WithResults() throws Exception {
        var user = buildUser();
        var response = sampleSpotResponse(user.getId());
        when(parkingSpotService.searchByRadius(-22.9068, -43.1729, 800.0))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/spots")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .param("lat", "-22.9068")
                        .param("lng", "-43.1729")
                        .param("radius", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Spot"));
    }

    @Test
    void search_shouldUseDefaultRadius() throws Exception {
        var user = buildUser();
        when(parkingSpotService.searchByRadius(-22.9068, -43.1729, 800.0))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/spots")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .param("lat", "-22.9068")
                        .param("lng", "-43.1729"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var response = sampleSpotResponse(user.getId());
        when(parkingSpotService.getById(spotId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/spots/" + spotId)
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Spot"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        when(parkingSpotService.getById(spotId)).thenThrow(new SpotNotFoundException(spotId.toString()));
        when(localizedMessageService.translate(any(SpotNotFoundException.class))).thenReturn("Parking spot not found");

        mockMvc.perform(get("/api/v1/spots/" + spotId)
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMySpots_shouldReturn200() throws Exception {
        var user = buildUser();
        var response = sampleSpotResponse(user.getId());
        when(parkingSpotService.getByUser(user.getId())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/spots/mine")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Spot"));
    }
}
