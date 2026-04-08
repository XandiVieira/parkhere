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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
}
