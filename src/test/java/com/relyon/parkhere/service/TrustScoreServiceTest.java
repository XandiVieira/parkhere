package com.relyon.parkhere.service;

import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.model.enums.TrustLevel;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private ParkingReportRepository reportRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @InjectMocks
    private TrustScoreService trustScoreService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private User buildUser(String email) {
        var user = User.builder()
                .id(UUID.randomUUID()).name("User").email(email)
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
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

    private ParkingReport buildReport(ParkingSpot spot, User user, AvailabilityStatus status, double distance) {
        var report = ParkingReport.builder()
                .id(UUID.randomUUID()).parkingSpot(spot).user(user)
                .availabilityStatus(status).gpsDistanceMeters(distance).build();
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return report;
    }

    @Test
    void recalculate_shouldReturnZeroForNoReports() {
        var user = buildUser("john@test.com");
        var spot = buildSpot(user);
        when(reportRepository.findByParkingSpotIdAndCreatedAtAfter(eq(spot.getId()), any()))
                .thenReturn(List.of());
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        var score = trustScoreService.recalculate(spot);

        assertEquals(0.0, score);
        assertEquals(0.0, spot.getTrustScore());
    }

    @Test
    void recalculate_shouldReturnHighScoreForManyRecentDiverseReports() {
        var user1 = buildUser("user1@test.com");
        var user2 = buildUser("user2@test.com");
        var user3 = buildUser("user3@test.com");
        var user4 = buildUser("user4@test.com");
        var user5 = buildUser("user5@test.com");
        var spot = buildSpot(user1);

        var reports = List.of(
                buildReport(spot, user1, AvailabilityStatus.AVAILABLE, 10.0),
                buildReport(spot, user2, AvailabilityStatus.AVAILABLE, 20.0),
                buildReport(spot, user3, AvailabilityStatus.AVAILABLE, 15.0),
                buildReport(spot, user4, AvailabilityStatus.AVAILABLE, 30.0),
                buildReport(spot, user5, AvailabilityStatus.AVAILABLE, 5.0),
                buildReport(spot, user1, AvailabilityStatus.AVAILABLE, 50.0),
                buildReport(spot, user2, AvailabilityStatus.AVAILABLE, 25.0),
                buildReport(spot, user3, AvailabilityStatus.AVAILABLE, 40.0),
                buildReport(spot, user4, AvailabilityStatus.AVAILABLE, 35.0),
                buildReport(spot, user5, AvailabilityStatus.AVAILABLE, 60.0)
        );

        when(reportRepository.findByParkingSpotIdAndCreatedAtAfter(eq(spot.getId()), any()))
                .thenReturn(reports);
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        var score = trustScoreService.recalculate(spot);

        assertTrue(score >= 0.7, "Score should be HIGH: " + score);
        assertEquals(TrustLevel.HIGH, TrustLevel.fromScore(score));
    }

    @Test
    void recalculate_shouldReturnLowScoreForSingleOldReport() {
        var user = buildUser("john@test.com");
        var spot = buildSpot(user);
        var report = buildReport(spot, user, AvailabilityStatus.AVAILABLE, 400.0);
        report.setCreatedAt(LocalDateTime.now().minusHours(60));

        when(reportRepository.findByParkingSpotIdAndCreatedAtAfter(eq(spot.getId()), any()))
                .thenReturn(List.of(report));
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        var score = trustScoreService.recalculate(spot);

        assertTrue(score < 0.4, "Score should be LOW for old single distant report: " + score);
    }

    @Test
    void recalculate_shouldWeightProximityCorrectly() {
        var user1 = buildUser("user1@test.com");
        var user2 = buildUser("user2@test.com");
        var spot = buildSpot(user1);

        var closeReport = buildReport(spot, user1, AvailabilityStatus.AVAILABLE, 10.0);
        var farReport = buildReport(spot, user2, AvailabilityStatus.AVAILABLE, 490.0);

        when(reportRepository.findByParkingSpotIdAndCreatedAtAfter(eq(spot.getId()), any()))
                .thenReturn(List.of(closeReport, farReport));
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        trustScoreService.recalculate(spot);

        var proximityFactor = TrustScoreService.calculateProximityFactor(List.of(closeReport, farReport));
        var closeProximity = 1.0 - (10.0 / 500.0);
        var farProximity = 1.0 - (490.0 / 500.0);
        var expected = (closeProximity + farProximity) / 2.0;
        assertEquals(expected, proximityFactor, 0.001);
    }

    @Test
    void recalculate_shouldHandleConsistencyCalculation() {
        var user1 = buildUser("user1@test.com");
        var user2 = buildUser("user2@test.com");
        var spot = buildSpot(user1);

        var available1 = buildReport(spot, user1, AvailabilityStatus.AVAILABLE, 50.0);
        var available2 = buildReport(spot, user2, AvailabilityStatus.AVAILABLE, 50.0);
        var unavailable = buildReport(spot, user1, AvailabilityStatus.UNAVAILABLE, 50.0);

        var consistency = TrustScoreService.calculateConsistencyFactor(List.of(available1, available2, unavailable));

        assertEquals(2.0 / 3.0, consistency, 0.001);
    }

    @Test
    void toTrustLevel_shouldMapCorrectLevels() {
        assertEquals(TrustLevel.HIGH, TrustLevel.fromScore(0.7));
        assertEquals(TrustLevel.HIGH, TrustLevel.fromScore(1.0));
        assertEquals(TrustLevel.MEDIUM, TrustLevel.fromScore(0.4));
        assertEquals(TrustLevel.MEDIUM, TrustLevel.fromScore(0.69));
        assertEquals(TrustLevel.LOW, TrustLevel.fromScore(0.1));
        assertEquals(TrustLevel.LOW, TrustLevel.fromScore(0.39));
        assertEquals(TrustLevel.NO_DATA, TrustLevel.fromScore(0.09));
        assertEquals(TrustLevel.NO_DATA, TrustLevel.fromScore(0.0));
    }

    @Test
    void calculateConfirmationFactor_shouldSaturateAtMax() {
        assertEquals(0.0, TrustScoreService.calculateConfirmationFactor(0));
        assertEquals(0.5, TrustScoreService.calculateConfirmationFactor(5));
        assertEquals(1.0, TrustScoreService.calculateConfirmationFactor(10));
        assertEquals(1.0, TrustScoreService.calculateConfirmationFactor(20));
    }
}
