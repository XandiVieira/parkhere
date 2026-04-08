package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateRemovalRequest;
import com.relyon.parkhere.exception.*;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.SpotRemovalConfirmation;
import com.relyon.parkhere.model.SpotRemovalRequest;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.RemovalStatus;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.SpotRemovalConfirmationRepository;
import com.relyon.parkhere.repository.SpotRemovalRequestRepository;
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
class SpotRemovalServiceTest {

    @Mock
    private SpotRemovalRequestRepository removalRequestRepository;

    @Mock
    private SpotRemovalConfirmationRepository confirmationRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @InjectMocks
    private SpotRemovalService spotRemovalService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User buildUser(String name, String email) {
        var user = User.builder()
                .id(UUID.randomUUID()).name(name).email(email)
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private ParkingSpot buildSpot(User user) {
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID()).name("Test Spot").type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(-43.1729, -22.9068)))
                .priceMin(5.0).priceMax(15.0).active(true).createdBy(user).build();
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        return spot;
    }

    private SpotRemovalRequest buildRemovalRequest(ParkingSpot spot, User requester) {
        var request = SpotRemovalRequest.builder()
                .id(UUID.randomUUID()).parkingSpot(spot).requestedBy(requester)
                .reason("Spot no longer exists").status(RemovalStatus.PENDING)
                .confirmationsNeeded(3).build();
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return request;
    }

    @Test
    void requestRemoval_shouldCreatePendingRequest() {
        var user = buildUser();
        var spot = buildSpot(user);
        var createRequest = new CreateRemovalRequest("Spot no longer exists");

        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(removalRequestRepository.findByParkingSpotIdAndStatusAndRequestedById(spot.getId(), RemovalStatus.PENDING, user.getId()))
                .thenReturn(Optional.empty());
        when(removalRequestRepository.save(any(SpotRemovalRequest.class))).thenAnswer(inv -> {
            var req = inv.<SpotRemovalRequest>getArgument(0);
            req.setId(UUID.randomUUID());
            req.setCreatedAt(LocalDateTime.now());
            req.setUpdatedAt(LocalDateTime.now());
            return req;
        });

        var response = spotRemovalService.requestRemoval(spot.getId(), createRequest, user);

        assertNotNull(response);
        assertEquals(RemovalStatus.PENDING, response.status());
        assertEquals("Spot no longer exists", response.reason());
        assertEquals(0, response.confirmationCount());
        verify(removalRequestRepository).save(any(SpotRemovalRequest.class));
    }

    @Test
    void requestRemoval_shouldThrowWhenSpotNotFound() {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        var createRequest = new CreateRemovalRequest("Gone");

        when(parkingSpotRepository.findByIdAndActiveTrue(spotId)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> spotRemovalService.requestRemoval(spotId, createRequest, user));
    }

    @Test
    void requestRemoval_shouldThrowWhenDuplicate() {
        var user = buildUser();
        var spot = buildSpot(user);
        var existingRequest = buildRemovalRequest(spot, user);
        var createRequest = new CreateRemovalRequest("Already requested");

        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(removalRequestRepository.findByParkingSpotIdAndStatusAndRequestedById(spot.getId(), RemovalStatus.PENDING, user.getId()))
                .thenReturn(Optional.of(existingRequest));

        assertThrows(DuplicateRemovalRequestException.class, () -> spotRemovalService.requestRemoval(spot.getId(), createRequest, user));
    }

    @Test
    void confirmRemoval_shouldAddConfirmation() {
        var requester = buildUser("Requester", "requester@test.com");
        var confirmer = buildUser("Confirmer", "confirmer@test.com");
        var spot = buildSpot(requester);
        var removalRequest = buildRemovalRequest(spot, requester);

        when(removalRequestRepository.findById(removalRequest.getId())).thenReturn(Optional.of(removalRequest));
        when(confirmationRepository.existsByRemovalRequestIdAndConfirmedById(removalRequest.getId(), confirmer.getId())).thenReturn(false);
        when(confirmationRepository.save(any(SpotRemovalConfirmation.class))).thenAnswer(inv -> {
            var conf = inv.<SpotRemovalConfirmation>getArgument(0);
            conf.setId(UUID.randomUUID());
            conf.setCreatedAt(LocalDateTime.now());
            conf.setUpdatedAt(LocalDateTime.now());
            return conf;
        });
        when(confirmationRepository.countByRemovalRequestId(removalRequest.getId())).thenReturn(1L);

        var response = spotRemovalService.confirmRemoval(removalRequest.getId(), confirmer);

        assertNotNull(response);
        assertEquals(1, response.confirmationCount());
        assertEquals(RemovalStatus.PENDING, response.status());
        verify(confirmationRepository).save(any(SpotRemovalConfirmation.class));
        verify(parkingSpotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void confirmRemoval_shouldDeactivateSpotWhenThresholdReached() {
        var requester = buildUser("Requester", "requester@test.com");
        var confirmer = buildUser("Confirmer", "confirmer@test.com");
        var spot = buildSpot(requester);
        var removalRequest = buildRemovalRequest(spot, requester);

        when(removalRequestRepository.findById(removalRequest.getId())).thenReturn(Optional.of(removalRequest));
        when(confirmationRepository.existsByRemovalRequestIdAndConfirmedById(removalRequest.getId(), confirmer.getId())).thenReturn(false);
        when(confirmationRepository.save(any(SpotRemovalConfirmation.class))).thenAnswer(inv -> {
            var conf = inv.<SpotRemovalConfirmation>getArgument(0);
            conf.setId(UUID.randomUUID());
            conf.setCreatedAt(LocalDateTime.now());
            conf.setUpdatedAt(LocalDateTime.now());
            return conf;
        });
        when(confirmationRepository.countByRemovalRequestId(removalRequest.getId())).thenReturn(3L);
        when(removalRequestRepository.save(any(SpotRemovalRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = spotRemovalService.confirmRemoval(removalRequest.getId(), confirmer);

        assertNotNull(response);
        assertEquals(3, response.confirmationCount());
        assertEquals(RemovalStatus.CONFIRMED, response.status());
        assertFalse(spot.isActive());
        verify(parkingSpotRepository).save(spot);
        verify(removalRequestRepository).save(removalRequest);
    }

    @Test
    void confirmRemoval_shouldThrowForSelfConfirmation() {
        var requester = buildUser();
        var spot = buildSpot(requester);
        var removalRequest = buildRemovalRequest(spot, requester);

        when(removalRequestRepository.findById(removalRequest.getId())).thenReturn(Optional.of(removalRequest));

        assertThrows(SelfConfirmationException.class, () -> spotRemovalService.confirmRemoval(removalRequest.getId(), requester));
    }

    @Test
    void confirmRemoval_shouldThrowWhenAlreadyConfirmed() {
        var requester = buildUser("Requester", "requester@test.com");
        var confirmer = buildUser("Confirmer", "confirmer@test.com");
        var spot = buildSpot(requester);
        var removalRequest = buildRemovalRequest(spot, requester);

        when(removalRequestRepository.findById(removalRequest.getId())).thenReturn(Optional.of(removalRequest));
        when(confirmationRepository.existsByRemovalRequestIdAndConfirmedById(removalRequest.getId(), confirmer.getId())).thenReturn(true);

        assertThrows(AlreadyConfirmedException.class, () -> spotRemovalService.confirmRemoval(removalRequest.getId(), confirmer));
    }

    @Test
    void confirmRemoval_shouldThrowWhenRequestNotFound() {
        var user = buildUser();
        var requestId = UUID.randomUUID();

        when(removalRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(RemovalRequestNotFoundException.class, () -> spotRemovalService.confirmRemoval(requestId, user));
    }

    @Test
    void getPendingRemovals_shouldReturnPendingOnly() {
        var user = buildUser();
        var spot = buildSpot(user);
        var pendingRequest = buildRemovalRequest(spot, user);

        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(removalRequestRepository.findByParkingSpotIdAndStatus(spot.getId(), RemovalStatus.PENDING))
                .thenReturn(List.of(pendingRequest));
        when(confirmationRepository.countByRemovalRequestId(pendingRequest.getId())).thenReturn(1L);

        var results = spotRemovalService.getPendingRemovals(spot.getId());

        assertEquals(1, results.size());
        assertEquals(RemovalStatus.PENDING, results.getFirst().status());
        assertEquals(1, results.getFirst().confirmationCount());
    }
}
