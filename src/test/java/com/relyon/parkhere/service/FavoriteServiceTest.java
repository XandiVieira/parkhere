package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.AlreadyFavoritedException;
import com.relyon.parkhere.exception.FavoriteNotFoundException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.UserFavorite;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.UserFavoriteRepository;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private UserFavoriteRepository userFavoriteRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @InjectMocks
    private FavoriteService favoriteService;

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

    private ParkingSpot buildSpot(User createdBy) {
        var spot = ParkingSpot.builder()
                .id(UUID.randomUUID())
                .name("Test Spot")
                .type(SpotType.STREET)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(-43.1729, -22.9068)))
                .priceMin(5.0)
                .priceMax(15.0)
                .createdBy(createdBy)
                .build();
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        return spot;
    }

    @Test
    void addFavorite_shouldSaveFavorite() {
        var user = buildUser();
        var spot = buildSpot(user);
        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(userFavoriteRepository.existsByUserIdAndParkingSpotId(user.getId(), spot.getId())).thenReturn(false);
        when(userFavoriteRepository.save(any(UserFavorite.class))).thenAnswer(inv -> inv.getArgument(0));

        favoriteService.addFavorite(spot.getId(), user);

        verify(userFavoriteRepository).save(any(UserFavorite.class));
    }

    @Test
    void addFavorite_shouldThrowWhenAlreadyFavorited() {
        var user = buildUser();
        var spot = buildSpot(user);
        when(parkingSpotRepository.findByIdAndActiveTrue(spot.getId())).thenReturn(Optional.of(spot));
        when(userFavoriteRepository.existsByUserIdAndParkingSpotId(user.getId(), spot.getId())).thenReturn(true);

        assertThrows(AlreadyFavoritedException.class, () -> favoriteService.addFavorite(spot.getId(), user));
        verify(userFavoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_shouldThrowWhenSpotNotFound() {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        when(parkingSpotRepository.findByIdAndActiveTrue(spotId)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> favoriteService.addFavorite(spotId, user));
        verify(userFavoriteRepository, never()).save(any());
    }

    @Test
    void removeFavorite_shouldDelete() {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        when(userFavoriteRepository.existsByUserIdAndParkingSpotId(user.getId(), spotId)).thenReturn(true);

        favoriteService.removeFavorite(spotId, user);

        verify(userFavoriteRepository).deleteByUserIdAndParkingSpotId(user.getId(), spotId);
    }

    @Test
    void removeFavorite_shouldThrowWhenNotFavorited() {
        var user = buildUser();
        var spotId = UUID.randomUUID();
        when(userFavoriteRepository.existsByUserIdAndParkingSpotId(user.getId(), spotId)).thenReturn(false);

        assertThrows(FavoriteNotFoundException.class, () -> favoriteService.removeFavorite(spotId, user));
        verify(userFavoriteRepository, never()).deleteByUserIdAndParkingSpotId(any(), any());
    }
}
