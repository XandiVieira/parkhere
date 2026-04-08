package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.ReportNotFoundException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.exception.UserNotFoundException;
import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private ParkingReportRepository parkingReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private User buildUser() {
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

    private ParkingSpot buildSpot() {
        var user = buildUser();
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID())
                .name("Test Spot")
                .type(SpotType.STREET)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(-43.1729, -22.9068)))
                .priceMin(5.0)
                .priceMax(15.0)
                .active(true)
                .createdBy(user)
                .build();
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        return spot;
    }

    private ParkingReport buildReport() {
        var user = buildUser();
        var spot = buildSpot();
        var report = ParkingReport.builder()
                .id(UUID.randomUUID())
                .parkingSpot(spot)
                .user(user)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .gpsDistanceMeters(10.0)
                .build();
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return report;
    }

    @Test
    void deactivateSpot_shouldSetInactive() {
        var spot = buildSpot();
        when(parkingSpotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.deactivateSpot(spot.getId());

        assertFalse(spot.isActive());
        verify(parkingSpotRepository).save(spot);
    }

    @Test
    void deactivateSpot_shouldThrowWhenNotFound() {
        var spotId = UUID.randomUUID();
        when(parkingSpotRepository.findById(spotId)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> adminService.deactivateSpot(spotId));
    }

    @Test
    void deleteReport_shouldDelete() {
        var report = buildReport();
        when(parkingReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        adminService.deleteReport(report.getId());

        verify(parkingReportRepository).delete(report);
    }

    @Test
    void banUser_shouldSetInactive() {
        var user = buildUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.banUser(user.getId());

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void unbanUser_shouldSetActive() {
        var user = buildUser();
        user.setActive(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.unbanUser(user.getId());

        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }
}
