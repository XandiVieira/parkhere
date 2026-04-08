package com.relyon.parkhere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.request.CreateReportRequest;
import com.relyon.parkhere.dto.response.ReportImageResponse;
import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotSummaryResponse;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.model.enums.TrustLevel;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.ParkingReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingReportController.class)
@Import(SecurityConfig.class)
class ParkingReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ParkingReportService reportService;

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
    void submitReport_shouldReturn201() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, 10.0, 4, false, "Good spot",
                -22.9070, -43.1730
        );
        var response = new ReportResponse(
                UUID.randomUUID(), spotId, user.getId(),
                AvailabilityStatus.AVAILABLE, 10.0, 4, false, "Good spot",
                50.0, List.of(), LocalDateTime.now()
        );
        when(reportService.submitReport(eq(spotId), any(CreateReportRequest.class), any(User.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/spots/" + spotId + "/reports")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.estimatedPrice").value(10.0));
    }

    @Test
    void submitReport_shouldReturn401WhenUnauthenticated() throws Exception {
        var spotId = UUID.randomUUID();
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, null, null, false, null,
                -22.9070, -43.1730
        );

        mockMvc.perform(post("/api/v1/spots/" + spotId + "/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitReport_shouldReturn404WhenSpotNotFound() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, null, null, false, null,
                -22.9070, -43.1730
        );
        when(reportService.submitReport(eq(spotId), any(CreateReportRequest.class), any(User.class), any()))
                .thenThrow(new SpotNotFoundException(spotId.toString()));
        when(localizedMessageService.translate(any(SpotNotFoundException.class)))
                .thenReturn("Parking spot not found");

        mockMvc.perform(post("/api/v1/spots/" + spotId + "/reports")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReports_shouldReturn200() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var response = new ReportResponse(
                UUID.randomUUID(), spotId, user.getId(),
                AvailabilityStatus.AVAILABLE, 10.0, 4, false, null,
                50.0, List.of(), LocalDateTime.now()
        );
        var pageResult = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
        when(reportService.getReportsForSpot(eq(spotId), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/spots/" + spotId + "/reports")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availabilityStatus").value("AVAILABLE"));
    }

    @Test
    void getSummary_shouldReturn200() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var summary = new SpotSummaryResponse(
                spotId, "Test Spot", SpotType.STREET,
                -22.9068, -43.1729, 5.0, 15.0, false,
                "Rua Test, 123", 0.75, TrustLevel.HIGH, 10, LocalDateTime.now(),
                AvailabilityStatus.AVAILABLE,
                10.0, 4.0, 20.0, false, 0.8, LocalDateTime.now()
        );
        when(reportService.getSummary(spotId)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/spots/" + spotId + "/summary")
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dominantAvailability").value("AVAILABLE"))
                .andExpect(jsonPath("$.totalConfirmations").value(10));
    }

    @Test
    void submitReport_multipart_shouldReturn201() throws Exception {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var response = new ReportResponse(
                UUID.randomUUID(), spotId, user.getId(),
                AvailabilityStatus.AVAILABLE, 10.0, 4, false, "Good spot",
                50.0, List.of(new ReportImageResponse("image1.jpg", "photo.jpg", "image/jpeg")), LocalDateTime.now()
        );

        var reportJson = objectMapper.writeValueAsString(new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, 10.0, 4, false, "Good spot",
                -22.9070, -43.1730
        ));

        var reportPart = new MockMultipartFile(
                "report", "report.json", "application/json", reportJson.getBytes()
        );
        var imagePart = new MockMultipartFile(
                "images", "test-image.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        when(reportService.submitReport(eq(spotId), any(CreateReportRequest.class), any(User.class), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/spots/" + spotId + "/reports")
                        .file(reportPart)
                        .file(imagePart)
                        .with(SecurityMockMvcRequestPostProcessors.user(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.images[0].filename").value("image1.jpg"));
    }

    @Test
    void submitReport_multipart_shouldReturn401WhenUnauthenticated() throws Exception {
        var spotId = UUID.randomUUID();
        var reportJson = objectMapper.writeValueAsString(new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, null, null, false, null,
                -22.9070, -43.1730
        ));
        var reportPart = new MockMultipartFile(
                "report", "report.json", "application/json", reportJson.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/spots/" + spotId + "/reports")
                        .file(reportPart))
                .andExpect(status().isUnauthorized());
    }
}
