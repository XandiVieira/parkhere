package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.SpotAnalytics;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.SpotAnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
class SpotAnalyticsServiceTest {

    @Mock
    private SpotAnalyticsRepository analyticsRepository;

    @Mock
    private ParkingReportRepository reportRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @InjectMocks
    private SpotAnalyticsService spotAnalyticsService;

    @Captor
    private ArgumentCaptor<List<SpotAnalytics>> analyticsCaptor;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

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

    private ParkingSpot buildSpot() {
        var user = buildUser();
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID())
                .name("Test Spot")
                .type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(-43.1729, -22.9068)))
                .priceMin(5.0)
                .priceMax(15.0)
                .trustScore(0.0)
                .totalConfirmations(0)
                .createdBy(user)
                .build();
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        return spot;
    }

    private ParkingReport buildReport(ParkingSpot spot, AvailabilityStatus status,
                                       Double price, Integer safety, boolean informal,
                                       LocalDateTime createdAt) {
        var report = ParkingReport.builder()
                .id(UUID.randomUUID())
                .parkingSpot(spot)
                .user(buildUser())
                .availabilityStatus(status)
                .estimatedPrice(price)
                .safetyRating(safety)
                .informalChargeReported(informal)
                .gpsDistanceMeters(10.0)
                .build();
        report.setCreatedAt(createdAt);
        report.setUpdatedAt(createdAt);
        return report;
    }

    @Test
    void computeAnalytics_shouldGroupByDayAndHour() {
        var spot = buildSpot();
        var monday10am = LocalDateTime.of(2026, 4, 6, 10, 30);
        var tuesday14pm = LocalDateTime.of(2026, 4, 7, 14, 15);

        var report1 = buildReport(spot, AvailabilityStatus.AVAILABLE, 10.0, 4, false, monday10am);
        var report2 = buildReport(spot, AvailabilityStatus.UNAVAILABLE, 8.0, 3, true, tuesday14pm);

        when(reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spot.getId()))
                .thenReturn(List.of(report1, report2));

        spotAnalyticsService.computeAnalytics(spot);

        verify(analyticsRepository).deleteByParkingSpotId(spot.getId());
        verify(analyticsRepository).saveAll(analyticsCaptor.capture());

        var saved = analyticsCaptor.getValue();
        assertEquals(2, saved.size());

        var mondayEntry = saved.stream()
                .filter(a -> a.getDayOfWeek().equals("MONDAY"))
                .findFirst().orElseThrow();
        assertEquals(10, mondayEntry.getHourBucket());

        var tuesdayEntry = saved.stream()
                .filter(a -> a.getDayOfWeek().equals("TUESDAY"))
                .findFirst().orElseThrow();
        assertEquals(14, tuesdayEntry.getHourBucket());
    }

    @Test
    void computeAnalytics_shouldCalculateAvailabilityRate() {
        var spot = buildSpot();
        var monday10am = LocalDateTime.of(2026, 4, 6, 10, 0);

        var report1 = buildReport(spot, AvailabilityStatus.AVAILABLE, 10.0, 4, false, monday10am);
        var report2 = buildReport(spot, AvailabilityStatus.AVAILABLE, 12.0, 5, false, monday10am.plusMinutes(15));
        var report3 = buildReport(spot, AvailabilityStatus.UNAVAILABLE, null, 3, true, monday10am.plusMinutes(30));

        when(reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spot.getId()))
                .thenReturn(List.of(report1, report2, report3));

        spotAnalyticsService.computeAnalytics(spot);

        verify(analyticsRepository).saveAll(analyticsCaptor.capture());

        var saved = analyticsCaptor.getValue();
        assertEquals(1, saved.size());

        var entry = saved.getFirst();
        assertEquals("MONDAY", entry.getDayOfWeek());
        assertEquals(10, entry.getHourBucket());
        assertEquals(2.0 / 3.0, entry.getAvgAvailabilityRate(), 0.001);
        assertEquals(11.0, entry.getAvgPrice(), 0.001);
        assertEquals(4.0, entry.getAvgSafetyRating(), 0.001);
        assertEquals(1.0 / 3.0, entry.getInformalChargeRate(), 0.001);
        assertEquals(3, entry.getReportCount());
    }

    @Test
    void computeAnalytics_shouldHandleNoReports() {
        var spot = buildSpot();

        when(reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spot.getId()))
                .thenReturn(List.of());

        spotAnalyticsService.computeAnalytics(spot);

        verify(analyticsRepository, never()).deleteByParkingSpotId(any());
        verify(analyticsRepository, never()).saveAll(any());
    }

    @Test
    void getAnalytics_shouldReturnGroupedResponse() {
        var spot = buildSpot();
        var spotId = spot.getId();

        var analytics1 = SpotAnalytics.builder()
                .id(UUID.randomUUID())
                .parkingSpot(spot)
                .dayOfWeek("MONDAY")
                .hourBucket(10)
                .avgAvailabilityRate(0.75)
                .avgPrice(10.0)
                .avgSafetyRating(4.0)
                .informalChargeRate(0.1)
                .reportCount(5)
                .computedAt(LocalDateTime.now())
                .build();
        analytics1.setCreatedAt(LocalDateTime.now());
        analytics1.setUpdatedAt(LocalDateTime.now());

        var analytics2 = SpotAnalytics.builder()
                .id(UUID.randomUUID())
                .parkingSpot(spot)
                .dayOfWeek("MONDAY")
                .hourBucket(11)
                .avgAvailabilityRate(0.5)
                .avgPrice(12.0)
                .avgSafetyRating(3.5)
                .informalChargeRate(0.2)
                .reportCount(3)
                .computedAt(LocalDateTime.now())
                .build();
        analytics2.setCreatedAt(LocalDateTime.now());
        analytics2.setUpdatedAt(LocalDateTime.now());

        var analytics3 = SpotAnalytics.builder()
                .id(UUID.randomUUID())
                .parkingSpot(spot)
                .dayOfWeek("TUESDAY")
                .hourBucket(14)
                .avgAvailabilityRate(0.9)
                .avgPrice(8.0)
                .avgSafetyRating(5.0)
                .informalChargeRate(0.0)
                .reportCount(7)
                .computedAt(LocalDateTime.now())
                .build();
        analytics3.setCreatedAt(LocalDateTime.now());
        analytics3.setUpdatedAt(LocalDateTime.now());

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(analyticsRepository.findByParkingSpotIdOrderByDayOfWeekAscHourBucketAsc(spotId))
                .thenReturn(List.of(analytics1, analytics2, analytics3));

        var response = spotAnalyticsService.getAnalytics(spotId);

        assertEquals(spotId, response.spotId());
        assertEquals(2, response.days().size());

        var monday = response.days().stream()
                .filter(d -> d.dayOfWeek().equals("MONDAY"))
                .findFirst().orElseThrow();
        assertEquals(2, monday.hours().size());
        assertEquals(10, monday.hours().getFirst().hour());
        assertEquals(0.75, monday.hours().getFirst().availabilityRate());

        var tuesday = response.days().stream()
                .filter(d -> d.dayOfWeek().equals("TUESDAY"))
                .findFirst().orElseThrow();
        assertEquals(1, tuesday.hours().size());
        assertEquals(14, tuesday.hours().getFirst().hour());
    }

    @Test
    void getAnalytics_shouldThrowWhenSpotNotFound() {
        var spotId = UUID.randomUUID();
        when(spotRepository.findById(spotId)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> spotAnalyticsService.getAnalytics(spotId));
    }
}
