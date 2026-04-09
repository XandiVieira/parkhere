package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateReportRequest;
import com.relyon.parkhere.exception.ReportCooldownException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.repository.ReportImageRepository;
import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
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
class ParkingReportServiceTest {

    @Mock
    private ParkingReportRepository reportRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private ReputationService reputationService;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private List<ImageStorageService> imageStorageServices;

    @Mock
    private ReportImageRepository reportImageRepository;

    @InjectMocks
    private ParkingReportService reportService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private ParkingSpot buildSpot(User user) {
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID()).name("Test Spot").type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(-43.1729, -22.9068)))
                .priceMin(5.0).priceMax(15.0).totalConfirmations(0).createdBy(user).build();
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        return spot;
    }

    private ParkingReport buildReport(ParkingSpot spot, User user) {
        var report = ParkingReport.builder()
                .id(UUID.randomUUID()).parkingSpot(spot).user(user)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .estimatedPrice(10.0).safetyRating(4)
                .informalChargeReported(false).gpsDistanceMeters(50.0).build();
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return report;
    }

    @Test
    void submitReport_shouldSaveAndIncrementConfirmations() {
        var user = buildUser();
        var spot = buildSpot(user);
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, 10.0, 4, false, null, null, null, null, "Plenty of space",
                -22.9070, -43.1730
        );
        when(spotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(reportRepository.save(any(ParkingReport.class))).thenAnswer(inv -> {
            var report = inv.<ParkingReport>getArgument(0);
            report.setId(UUID.randomUUID());
            report.setCreatedAt(LocalDateTime.now());
            report.setUpdatedAt(LocalDateTime.now());
            return report;
        });

        var response = reportService.submitReport(spot.getId(), request, user, null);

        assertNotNull(response);
        assertEquals(AvailabilityStatus.AVAILABLE, response.availabilityStatus());
        assertEquals(10.0, response.estimatedPrice());
        assertEquals(1, spot.getTotalConfirmations());
        verify(reportRepository).save(any(ParkingReport.class));
        verify(trustScoreService).recalculate(spot);
    }

    @Test
    void submitReport_shouldThrowWhenSpotNotFound() {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, null, null, false, null, null, null, null, null,
                -22.9070, -43.1730
        );
        when(spotRepository.findByIdAndActiveTrue(spotId)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> reportService.submitReport(spotId, request, user, null));
    }

    @Test
    void submitReport_shouldCalculateGpsDistance() {
        var user = buildUser();
        var spot = buildSpot(user);
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, null, null, false, null, null, null, null, null,
                -22.9068, -43.1729
        );
        when(spotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(reportRepository.save(any(ParkingReport.class))).thenAnswer(inv -> {
            var report = inv.<ParkingReport>getArgument(0);
            report.setId(UUID.randomUUID());
            report.setCreatedAt(LocalDateTime.now());
            report.setUpdatedAt(LocalDateTime.now());
            return report;
        });

        var response = reportService.submitReport(spot.getId(), request, user, null);

        assertTrue(response.gpsDistanceMeters() < 1.0);
    }

    @Test
    void getReportsForSpot_shouldReturnMappedReports() {
        var user = buildUser();
        var spot = buildSpot(user);
        var report = buildReport(spot, user);
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(spotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spot.getId(), pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(report)));

        var results = reportService.getReportsForSpot(spot.getId(), pageable);

        assertEquals(1, results.getContent().size());
        assertEquals(AvailabilityStatus.AVAILABLE, results.getContent().getFirst().availabilityStatus());
    }

    @Test
    void getSummary_shouldAggregateReports() {
        var user = buildUser();
        var spot = buildSpot(user);
        spot.setTotalConfirmations(3);

        var report1 = buildReport(spot, user);
        var report2 = buildReport(spot, user);
        report2.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        report2.setEstimatedPrice(12.0);
        report2.setSafetyRating(3);
        report2.setInformalChargeReported(true);

        var report3 = buildReport(spot, user);
        report3.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        when(spotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(reportRepository.findByParkingSpotIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(spot.getId()), any()))
                .thenReturn(List.of(report1, report2, report3));

        var summary = reportService.getSummary(spot.getId());

        assertEquals(spot.getId(), summary.spotId());
        assertEquals(AvailabilityStatus.AVAILABLE, summary.dominantAvailability());
        assertNotNull(summary.avgEstimatedPrice());
        assertNotNull(summary.avgSafetyRating());
        assertTrue(summary.informalChargePercentage() > 0);
        assertEquals(3, summary.totalConfirmations());
    }

    @Test
    void getSummary_shouldHandleNoReports() {
        var user = buildUser();
        var spot = buildSpot(user);
        when(spotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(reportRepository.findByParkingSpotIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(spot.getId()), any()))
                .thenReturn(List.of());

        var summary = reportService.getSummary(spot.getId());

        assertEquals(AvailabilityStatus.UNKNOWN, summary.dominantAvailability());
        assertNull(summary.avgEstimatedPrice());
        assertNull(summary.avgSafetyRating());
        assertEquals(0.0, summary.informalChargePercentage());
        assertNull(summary.lastReportAt());
    }

    @Test
    void calculateDistance_shouldReturnCorrectDistance() {
        var distance = ParkingReportService.calculateDistance(-22.9068, -43.1729, -22.9068, -43.1729);
        assertEquals(0.0, distance, 0.01);

        var distanceRio = ParkingReportService.calculateDistance(-22.9068, -43.1729, -22.9110, -43.1650);
        assertTrue(distanceRio > 500 && distanceRio < 1500);
    }

    @Test
    void submitReport_shouldThrowWhenCooldownActive() {
        var user = buildUser();
        var spot = buildSpot(user);
        var request = new CreateReportRequest(
                AvailabilityStatus.AVAILABLE, null, null, false, null, null, null, null, null,
                -22.9070, -43.1730
        );
        when(spotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(reportRepository.existsByParkingSpotIdAndUserIdAndCreatedAtAfter(eq(spot.getId()), eq(user.getId()), any()))
                .thenReturn(true);

        assertThrows(ReportCooldownException.class, () -> reportService.submitReport(spot.getId(), request, user, null));
        verify(reportRepository, never()).save(any());
    }
}
