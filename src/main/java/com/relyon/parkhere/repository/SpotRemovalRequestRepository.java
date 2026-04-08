package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.SpotRemovalRequest;
import com.relyon.parkhere.model.enums.RemovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotRemovalRequestRepository extends JpaRepository<SpotRemovalRequest, UUID> {

    Optional<SpotRemovalRequest> findByParkingSpotIdAndStatusAndRequestedById(UUID spotId, RemovalStatus status, UUID userId);

    List<SpotRemovalRequest> findByParkingSpotIdAndStatus(UUID spotId, RemovalStatus status);
}
