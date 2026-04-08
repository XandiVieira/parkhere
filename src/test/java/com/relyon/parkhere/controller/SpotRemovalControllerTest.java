package com.relyon.parkhere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.request.CreateRemovalRequest;
import com.relyon.parkhere.dto.response.RemovalRequestResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.RemovalStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.SpotRemovalService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpotRemovalController.class)
@Import(SecurityConfig.class)
class SpotRemovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SpotRemovalService spotRemovalService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LocalizedMessageService localizedMessageService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void requestRemoval_shouldReturn201() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var request = new CreateRemovalRequest("Spot no longer exists");
        var response = new RemovalRequestResponse(
                UUID.randomUUID(), spotId, user.getId(),
                "Spot no longer exists", RemovalStatus.PENDING, 0, 3, LocalDateTime.now()
        );

        when(spotRemovalService.requestRemoval(eq(spotId), any(CreateRemovalRequest.class), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/spots/" + spotId + "/removal-requests")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.confirmationCount").value(0))
                .andExpect(jsonPath("$.confirmationsNeeded").value(3));
    }

    @Test
    void confirmRemoval_shouldReturn200() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var response = new RemovalRequestResponse(
                requestId, spotId, UUID.randomUUID(),
                "Gone", RemovalStatus.PENDING, 1, 3, LocalDateTime.now()
        );

        when(spotRemovalService.confirmRemoval(eq(requestId), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/spots/" + spotId + "/removal-requests/" + requestId + "/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationCount").value(1));
    }

    @Test
    void getPendingRemovals_shouldReturn200() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var response = new RemovalRequestResponse(
                UUID.randomUUID(), spotId, user.getId(),
                "Spot gone", RemovalStatus.PENDING, 2, 3, LocalDateTime.now()
        );

        when(spotRemovalService.getPendingRemovals(spotId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/spots/" + spotId + "/removal-requests")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].confirmationCount").value(2));
    }

    @Test
    void requestRemoval_shouldReturn401WhenUnauthenticated() throws Exception {
        var spotId = UUID.randomUUID();
        var request = new CreateRemovalRequest("Gone");

        mockMvc.perform(post("/api/v1/spots/" + spotId + "/removal-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
