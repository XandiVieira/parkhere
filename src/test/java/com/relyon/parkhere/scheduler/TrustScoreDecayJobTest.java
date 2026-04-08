package com.relyon.parkhere.scheduler;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.service.TrustScoreService;
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
class TrustScoreDecayJobTest {

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private TrustScoreService trustScoreService;

    @InjectMocks
    private TrustScoreDecayJob decayJob;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private ParkingSpot buildSpot(double trustScore) {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID()).name("Test Spot").type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(-43.1729, -22.9068)))
                .trustScore(trustScore).active(true).createdBy(user).build();
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        return spot;
    }

    @Test
    void decayTrustScores_shouldRecalculateActiveSpotsWithScore() {
        var spot1 = buildSpot(0.8);
        var spot2 = buildSpot(0.3);
        when(spotRepository.findByActiveTrueAndTrustScoreGreaterThan(0.0))
                .thenReturn(List.of(spot1, spot2));
        when(trustScoreService.recalculate(spot1)).thenReturn(0.5);
        when(trustScoreService.recalculate(spot2)).thenReturn(0.1);

        decayJob.decayTrustScores();

        verify(trustScoreService).recalculate(spot1);
        verify(trustScoreService).recalculate(spot2);
    }

    @Test
    void decayTrustScores_shouldHandleEmptyList() {
        when(spotRepository.findByActiveTrueAndTrustScoreGreaterThan(0.0))
                .thenReturn(List.of());

        decayJob.decayTrustScores();

        verify(trustScoreService, never()).recalculate(any());
    }
}
