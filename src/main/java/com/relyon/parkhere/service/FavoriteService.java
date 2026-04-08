package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.exception.AlreadyFavoritedException;
import com.relyon.parkhere.exception.FavoriteNotFoundException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.UserFavorite;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.UserFavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    @Transactional
    public void addFavorite(UUID spotId, User user) {
        var spot = parkingSpotRepository.findByIdAndActiveTrue(spotId)
                .orElseThrow(() -> new SpotNotFoundException(spotId.toString()));

        if (userFavoriteRepository.existsByUserIdAndParkingSpotId(user.getId(), spotId)) {
            throw new AlreadyFavoritedException();
        }

        var favorite = UserFavorite.builder()
                .user(user)
                .parkingSpot(spot)
                .build();
        userFavoriteRepository.save(favorite);
        log.info("User {} added spot {} to favorites", user.getEmail(), spotId);
    }

    @Transactional
    public void removeFavorite(UUID spotId, User user) {
        if (!userFavoriteRepository.existsByUserIdAndParkingSpotId(user.getId(), spotId)) {
            throw new FavoriteNotFoundException();
        }

        userFavoriteRepository.deleteByUserIdAndParkingSpotId(user.getId(), spotId);
        log.info("User {} removed spot {} from favorites", user.getEmail(), spotId);
    }

    @Transactional(readOnly = true)
    public Page<SpotResponse> getFavorites(User user, Pageable pageable) {
        return userFavoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(fav -> SpotResponse.from(fav.getParkingSpot()));
    }
}
