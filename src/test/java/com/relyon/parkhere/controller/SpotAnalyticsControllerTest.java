package com.relyon.parkhere.controller;

import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.response.SpotAnalyticsResponse;
import com.relyon.parkhere.dto.response.SpotAnalyticsResponse.DayAnalytics;
import com.relyon.parkhere.dto.response.SpotAnalyticsResponse.HourAnalytics;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.SpotAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpotAnalyticsController.class)
@Import(SecurityConfig.class)
class SpotAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpotAnalyticsService analyticsService;

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
    void getAnalytics_shouldReturn200() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();

        var hours = List.of(new HourAnalytics(10, 0.75, 10.0, 4.0, 0.1, 5));
        var days = List.of(new DayAnalytics("MONDAY", hours));
        var response = new SpotAnalyticsResponse(spotId, days);

        when(analyticsService.getAnalytics(spotId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/spots/" + spotId + "/analytics")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotId").value(spotId.toString()))
                .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].hours[0].hour").value(10))
                .andExpect(jsonPath("$.days[0].hours[0].availabilityRate").value(0.75))
                .andExpect(jsonPath("$.days[0].hours[0].reportCount").value(5));
    }

    @Test
    void getAnalytics_shouldReturn404WhenSpotNotFound() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();

        when(analyticsService.getAnalytics(spotId)).thenThrow(new SpotNotFoundException(spotId.toString()));
        when(localizedMessageService.translate(any(SpotNotFoundException.class))).thenReturn("Parking spot not found");

        mockMvc.perform(get("/api/v1/spots/" + spotId + "/analytics")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isNotFound());
    }
}
