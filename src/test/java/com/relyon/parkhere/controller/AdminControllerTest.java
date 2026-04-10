package com.relyon.parkhere.controller;

import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.AdminService;
import com.relyon.parkhere.service.LocalizedMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.model.enums.TrustLevel;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LocalizedMessageService localizedMessageService;

    private User buildAdminUser() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("Admin")
                .email("admin@test.com")
                .password("encoded")
                .role(Role.ADMIN)
                .active(true)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User buildRegularUser() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .role(Role.USER)
                .active(true)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void deactivateSpot_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var spotId = UUID.randomUUID();
        when(localizedMessageService.translate(anyString())).thenReturn("Parking spot deactivated");

        mockMvc.perform(put("/api/v1/admin/spots/" + spotId + "/deactivate")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Parking spot deactivated"));
    }

    @Test
    void deactivateSpot_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();
        var spotId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/admin/spots/" + spotId + "/deactivate")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void banUser_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var userId = UUID.randomUUID();
        when(localizedMessageService.translate(anyString())).thenReturn("User banned");

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/ban")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User banned"));
    }

    @Test
    void banUser_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();
        var userId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/ban")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unbanUser_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var userId = UUID.randomUUID();
        when(localizedMessageService.translate(anyString())).thenReturn("User unbanned");

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/unban")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User unbanned"));
    }

    @Test
    void unbanUser_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();
        var userId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/unban")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReport_shouldReturn204ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var reportId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/reports/" + reportId)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isNoContent());

        verify(adminService).deleteReport(reportId);
    }

    @Test
    void deleteReport_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();
        var reportId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/reports/" + reportId)
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateSpot_shouldReturn401WhenUnauthenticated() throws Exception {
        var spotId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/admin/spots/" + spotId + "/deactivate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var userResponse = new UserResponse(UUID.randomUUID(), "John", null, "john@test.com", Role.USER, 0.0, null, false, LocalDateTime.now());
        when(adminService.listUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(userResponse)));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John"));
    }

    @Test
    void listUsers_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSpots_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var spotResponse = new SpotResponse(UUID.randomUUID(), "Test Spot", SpotType.STREET, -22.9068, -43.1729, 5.0, 15.0, false, null, null, 50.0, TrustLevel.MEDIUM, 3, null, "Porto Alegre", "UNKNOWN", null, List.of(), UUID.randomUUID(), LocalDateTime.now());
        when(adminService.listSpots(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spotResponse)));

        mockMvc.perform(get("/api/v1/admin/spots")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Spot"));
    }

    @Test
    void listSpots_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();

        mockMvc.perform(get("/api/v1/admin/spots")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listReports_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        var reportResponse = new ReportResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AvailabilityStatus.AVAILABLE, 10.0, 4, false, null, null, null, null, "Good spot", 50.0, List.of(), LocalDateTime.now());
        when(adminService.listReports(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(reportResponse)));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].note").value("Good spot"));
    }

    @Test
    void listReports_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();

        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStats_shouldReturn200ForAdmin() throws Exception {
        var admin = buildAdminUser();
        when(adminService.getStats()).thenReturn(Map.of("totalUsers", 10L, "totalSpots", 25L, "totalReports", 50L));

        mockMvc.perform(get("/api/v1/admin/stats")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.totalSpots").value(25))
                .andExpect(jsonPath("$.totalReports").value(50));
    }

    @Test
    void getStats_shouldReturn403ForNonAdmin() throws Exception {
        var user = buildRegularUser();

        mockMvc.perform(get("/api/v1/admin/stats")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
