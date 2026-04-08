package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.SpotRemovalConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpotRemovalConfirmationRepository extends JpaRepository<SpotRemovalConfirmation, UUID> {

    boolean existsByRemovalRequestIdAndConfirmedById(UUID requestId, UUID userId);

    long countByRemovalRequestId(UUID requestId);

    long countByConfirmedById(UUID userId);
}
