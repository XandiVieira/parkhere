package com.relyon.parkhere.service;

import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.UserRepository;
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
class ReputationServiceTest {

    @Mock
    private ParkingReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReputationService reputationService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private User buildUser(int daysOld) {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).reputationScore(0.0).build();
        user.setCreatedAt(LocalDateTime.now().minusDays(daysOld));
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private ParkingSpot buildSpot(User user) {
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID()).name("Test Spot").type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(-43.1729, -22.9068)))
                .createdBy(user).build();
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
    void recalculate_shouldReturnZeroForNewUserWithNoReports() {
        var user = buildUser(0);
        when(reportRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var score = reputationService.recalculate(user);

        assertEquals(0.0, score);
        assertEquals(0.0, user.getReputationScore());
    }

    @Test
    void recalculate_shouldIncreaseWithMoreReports() {
        var user = buildUser(30);
        var spot = buildSpot(user);
        var reports = List.of(
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 50.0),
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 50.0),
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 50.0),
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 50.0),
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 50.0)
        );
        when(reportRepository.findByUserId(user.getId())).thenReturn(reports);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var score = reputationService.recalculate(user);

        assertTrue(score > 0);
        assertEquals(score, user.getReputationScore());
    }

    @Test
    void recalculate_shouldRewardCloseProximityReports() {
        var user = buildUser(30);
        var spot = buildSpot(user);

        var closeReports = List.of(
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 10.0),
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 20.0)
        );
        when(reportRepository.findByUserId(user.getId())).thenReturn(closeReports);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        var closeScore = reputationService.recalculate(user);

        var farReports = List.of(
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 450.0),
                buildReport(spot, user, AvailabilityStatus.AVAILABLE, 480.0)
        );
        when(reportRepository.findByUserId(user.getId())).thenReturn(farReports);
        var farScore = reputationService.recalculate(user);

        assertTrue(closeScore > farScore, "Close reports should yield higher reputation");
    }

    @Test
    void recalculate_shouldCapAtMaximum() {
        var user = buildUser(365);
        var spot = buildSpot(user);

        var manyReports = new java.util.ArrayList<ParkingReport>();
        for (int i = 0; i < 100; i++) {
            manyReports.add(buildReport(spot, user, AvailabilityStatus.AVAILABLE, 5.0));
        }
        when(reportRepository.findByUserId(user.getId())).thenReturn(manyReports);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var score = reputationService.recalculate(user);

        assertTrue(score <= 100.0, "Score should not exceed 100: " + score);
    }

    @Test
    void recalculate_shouldIncludeAccountAgeBonus() {
        var newUser = buildUser(1);
        var oldUser = buildUser(300);
        var spot = buildSpot(newUser);

        var reports = List.of(buildReport(spot, newUser, AvailabilityStatus.AVAILABLE, 50.0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        when(reportRepository.findByUserId(newUser.getId())).thenReturn(reports);
        var newScore = reputationService.recalculate(newUser);

        when(reportRepository.findByUserId(oldUser.getId())).thenReturn(reports);
        var oldScore = reputationService.recalculate(oldUser);

        assertTrue(oldScore > newScore, "Older accounts should have higher score");
    }
}
