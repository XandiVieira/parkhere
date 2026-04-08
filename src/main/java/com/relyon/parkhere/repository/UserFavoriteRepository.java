package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.UserFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UUID> {

    Page<UserFavorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndParkingSpotId(UUID userId, UUID spotId);

    void deleteByUserIdAndParkingSpotId(UUID userId, UUID spotId);
}
