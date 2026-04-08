package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.ParkingReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ParkingReportRepository extends JpaRepository<ParkingReport, UUID> {

    List<ParkingReport> findByParkingSpotIdOrderByCreatedAtDesc(UUID spotId);

    Page<ParkingReport> findByParkingSpotIdOrderByCreatedAtDesc(UUID spotId, Pageable pageable);

    List<ParkingReport> findByParkingSpotIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID spotId, LocalDateTime after);

    List<ParkingReport> findByParkingSpotIdAndCreatedAtAfter(UUID spotId, LocalDateTime after);

    List<ParkingReport> findByUserId(UUID userId);

    boolean existsByParkingSpotIdAndUserIdAndCreatedAtAfter(UUID spotId, UUID userId, LocalDateTime after);

    long countByParkingSpotId(UUID spotId);
}
