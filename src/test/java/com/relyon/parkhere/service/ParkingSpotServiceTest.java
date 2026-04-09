package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateSpotRequest;
import com.relyon.parkhere.dto.request.UpdateSpotRequest;
import com.relyon.parkhere.exception.NearbySpotExistsException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.exception.UnauthorizedSpotModificationException;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
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
class ParkingSpotServiceTest {

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @InjectMocks
    private ParkingSpotService parkingSpotService;

    @Mock
    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(parkingSpotService, "geocodingServices", List.of());
    }

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

    private ParkingSpot buildSpot(User user) {
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

    @Test
    void create_shouldSaveAndReturnSpotResponse() {
        var user = buildUser();
        var request = new CreateSpotRequest("Street Parking", SpotType.STREET, -22.9068, -43.1729, 5.0, 15.0, false, null, null, null, null);
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> {
            var spot = inv.<ParkingSpot>getArgument(0);
            spot.setId(UUID.randomUUID());
            spot.setCreatedAt(LocalDateTime.now());
            spot.setUpdatedAt(LocalDateTime.now());
            return spot;
        });

        var response = parkingSpotService.create(request, user, true);

        assertNotNull(response);
        assertEquals("Street Parking", response.name());
        assertEquals(SpotType.STREET, response.type());
        assertEquals(-22.9068, response.latitude(), 0.0001);
        assertEquals(-43.1729, response.longitude(), 0.0001);
        assertEquals(5.0, response.priceMin());
        assertEquals(15.0, response.priceMax());
        verify(parkingSpotRepository).save(any(ParkingSpot.class));
    }

    @Test
    void create_shouldThrowWhenNearbySpotExistsAndNotForced() {
        var user = buildUser();
        var existingSpot = buildSpot(user);
        var request = new CreateSpotRequest("Duplicate Spot", SpotType.STREET, -22.9068, -43.1729, 5.0, 15.0, false, null, null, null, null);
        when(parkingSpotRepository.findWithinRadius(-22.9068, -43.1729, 50.0))
                .thenReturn(List.of(existingSpot));

        var exception = assertThrows(NearbySpotExistsException.class,
                () -> parkingSpotService.create(request, user, false));

        assertEquals(1, exception.getNearbySpots().size());
        verify(parkingSpotRepository, never()).save(any());
    }

    @Test
    void create_shouldAllowWhenNearbySpotExistsAndForced() {
        var user = buildUser();
        var request = new CreateSpotRequest("Forced Spot", SpotType.STREET, -22.9068, -43.1729, 5.0, 15.0, false, null, null, null, null);
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> {
            var spot = inv.<ParkingSpot>getArgument(0);
            spot.setId(UUID.randomUUID());
            spot.setCreatedAt(LocalDateTime.now());
            spot.setUpdatedAt(LocalDateTime.now());
            return spot;
        });

        var response = parkingSpotService.create(request, user, true);

        assertNotNull(response);
        assertEquals("Forced Spot", response.name());
        verify(parkingSpotRepository).save(any(ParkingSpot.class));
    }

    @Test
    void searchByRadius_shouldReturnMappedResults() {
        var user = buildUser();
        var spot = buildSpot(user);
        when(parkingSpotRepository.findWithinRadius(-22.9068, -43.1729, 800))
                .thenReturn(List.of(spot));

        var results = parkingSpotService.searchByRadius(-22.9068, -43.1729, 800);

        assertEquals(1, results.size());
        assertEquals("Test Spot", results.getFirst().name());
    }

    @Test
    void searchByRadius_shouldReturnEmptyListWhenNoResults() {
        when(parkingSpotRepository.findWithinRadius(-22.0, -43.0, 100))
                .thenReturn(List.of());

        var results = parkingSpotService.searchByRadius(-22.0, -43.0, 100);

        assertTrue(results.isEmpty());
    }

    @Test
    void getById_shouldReturnSpot() {
        var user = buildUser();
        var spot = buildSpot(user);
        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));

        var response = parkingSpotService.getById(spot.getId());

        assertEquals(spot.getName(), response.name());
        assertEquals(spot.getCreatedBy().getId(), response.createdBy());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(parkingSpotRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> parkingSpotService.getById(id));
    }

    @Test
    void getByUser_shouldReturnUserSpots() {
        var user = buildUser();
        var spot = buildSpot(user);
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(parkingSpotRepository.findByCreatedByIdAndActiveTrue(user.getId(), pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(spot)));

        var results = parkingSpotService.getByUser(user.getId(), pageable);

        assertEquals(1, results.getContent().size());
        assertEquals("Test Spot", results.getContent().getFirst().name());
    }

    @Test
    void update_shouldUpdateAllFields() {
        var user = buildUser();
        var spot = buildSpot(user);
        var request = new UpdateSpotRequest("Updated Spot", 10.0, 25.0, true, 50, null, "Updated notes", null);
        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = parkingSpotService.update(spot.getId(), request, user);

        assertEquals("Updated Spot", response.name());
        assertEquals(10.0, response.priceMin());
        assertEquals(25.0, response.priceMax());
        assertTrue(response.requiresBooking());
        assertEquals(50, response.estimatedSpots());
        assertEquals("Updated notes", response.notes());
    }

    @Test
    void update_shouldThrowWhenNotCreator() {
        var creator = buildUser();
        var otherUser = User.builder()
                .id(UUID.randomUUID()).name("Other").email("other@test.com")
                .password("encoded").role(Role.USER).build();
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());
        var spot = buildSpot(creator);
        var request = new UpdateSpotRequest("Hacked", 0.0, 0.0, false, null, null, null, null);
        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));

        assertThrows(UnauthorizedSpotModificationException.class,
                () -> parkingSpotService.update(spot.getId(), request, otherUser));
    }

    @Test
    void update_shouldThrowWhenSpotNotFound() {
        var user = buildUser();
        var id = UUID.randomUUID();
        var request = new UpdateSpotRequest("Name", 0.0, 0.0, false, null, null, null, null);
        when(parkingSpotRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> parkingSpotService.update(id, request, user));
    }
}
