package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.ParkingSpot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID>, ParkingSpotRepositoryCustom {

    @Query(value = """
            SELECT ps.* FROM parking_spots ps
            WHERE ps.active = true
            AND ST_DWithin(
                ps.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
            ORDER BY ST_Distance(
                ps.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            )
            """, nativeQuery = true)
    List<ParkingSpot> findWithinRadius(@Param("lat") double lat,
                                       @Param("lng") double lng,
                                       @Param("radiusMeters") double radiusMeters);

    List<ParkingSpot> findByCreatedByIdAndActiveTrue(UUID userId);

    Page<ParkingSpot> findByCreatedByIdAndActiveTrue(UUID userId, Pageable pageable);

    Optional<ParkingSpot> findByIdAndActiveTrue(UUID id);

    List<ParkingSpot> findByActiveTrueAndTrustScoreGreaterThan(double minScore);
}
