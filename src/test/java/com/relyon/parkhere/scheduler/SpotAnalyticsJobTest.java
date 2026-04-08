package com.relyon.parkhere.scheduler;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.service.SpotAnalyticsService;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotAnalyticsJobTest {

    @Mock
    private SpotAnalyticsService analyticsService;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private ParkingReportRepository reportRepository;

    @InjectMocks
    private SpotAnalyticsJob spotAnalyticsJob;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private ParkingSpot buildSpot(UUID id) {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        var spot = ParkingSpot.builder()
                .id(id)
                .name("Spot " + id)
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

    @Test
    void runJob_shouldProcessActiveSpots() {
        var spot1Id = UUID.randomUUID();
        var spot2Id = UUID.randomUUID();
        var spot1 = buildSpot(spot1Id);
        var spot2 = buildSpot(spot2Id);

        when(spotRepository.findAll()).thenReturn(List.of(spot1, spot2));
        when(reportRepository.countByParkingSpotId(spot1Id)).thenReturn(5L);
        when(reportRepository.countByParkingSpotId(spot2Id)).thenReturn(0L);

        spotAnalyticsJob.run();

        verify(analyticsService).computeAnalytics(spot1);
        verify(analyticsService, never()).computeAnalytics(spot2);
    }

    @Test
    void runJob_shouldHandleEmptyList() {
        when(spotRepository.findAll()).thenReturn(List.of());

        spotAnalyticsJob.run();

        verify(analyticsService, never()).computeAnalytics(any());
    }
}
