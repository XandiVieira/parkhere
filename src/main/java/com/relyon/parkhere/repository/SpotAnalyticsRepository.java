package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.SpotAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SpotAnalyticsRepository extends JpaRepository<SpotAnalytics, UUID> {

    List<SpotAnalytics> findByParkingSpotIdOrderByDayOfWeekAscHourBucketAsc(UUID spotId);

    @Modifying
    @Transactional
    void deleteByParkingSpotId(UUID spotId);
}
