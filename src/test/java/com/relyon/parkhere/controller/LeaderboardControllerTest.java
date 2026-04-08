package com.relyon.parkhere.controller;

import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.response.LeaderboardResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LeaderboardService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardController.class)
@Import(SecurityConfig.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LocalizedMessageService localizedMessageService;

    @Test
    void getLeaderboard_shouldReturn200() throws Exception {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        var entry = new LeaderboardResponse.LeaderboardEntryResponse(1, "Alice", 100);
        var response = new LeaderboardResponse(LeaderboardPeriod.WEEKLY, "2026-W15",
                LeaderboardCategory.MOST_POINTS, List.of(entry));
        when(leaderboardService.getLeaderboard(eq(LeaderboardPeriod.WEEKLY), any(), eq(LeaderboardCategory.MOST_POINTS)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/leaderboards")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .param("period", "WEEKLY")
                        .param("category", "MOST_POINTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].userName").value("Alice"))
                .andExpect(jsonPath("$.entries[0].score").value(100));
    }
}
