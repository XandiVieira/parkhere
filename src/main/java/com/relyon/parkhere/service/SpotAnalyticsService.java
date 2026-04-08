package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.response.SpotAnalyticsResponse;
import com.relyon.parkhere.dto.response.SpotAnalyticsResponse.DayAnalytics;
import com.relyon.parkhere.dto.response.SpotAnalyticsResponse.HourAnalytics;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.SpotAnalytics;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.SpotAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotAnalyticsService {

    private final SpotAnalyticsRepository analyticsRepository;
    private final ParkingReportRepository reportRepository;
    private final ParkingSpotRepository spotRepository;

    @Transactional
    public void computeAnalytics(ParkingSpot spot) {
        var spotId = spot.getId();
        var reports = reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spotId);

        if (reports.isEmpty()) {
            log.debug("No reports found for spot {}, skipping analytics computation", spotId);
            return;
        }

        var grouped = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().getDayOfWeek().name() + "|" + r.getCreatedAt().getHour()
                ));

        var now = LocalDateTime.now();

        var analyticsEntries = grouped.entrySet().stream()
                .map(entry -> {
                    var parts = entry.getKey().split("\\|");
                    var dayOfWeek = parts[0];
                    var hourBucket = Integer.parseInt(parts[1]);
                    var group = entry.getValue();

                    var availableCount = group.stream()
                            .filter(r -> r.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                            .count();
                    var avgAvailabilityRate = (double) availableCount / group.size();

                    var avgPrice = group.stream()
                            .map(ParkingReport::getEstimatedPrice)
                            .filter(p -> p != null)
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(Double.NaN);

                    var avgSafety = group.stream()
                            .map(ParkingReport::getSafetyRating)
                            .filter(r -> r != null)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(Double.NaN);

                    var informalCount = group.stream()
                            .filter(ParkingReport::isInformalChargeReported)
                            .count();
                    var informalChargeRate = (double) informalCount / group.size();

                    return SpotAnalytics.builder()
                            .parkingSpot(spot)
                            .dayOfWeek(dayOfWeek)
                            .hourBucket(hourBucket)
                            .avgAvailabilityRate(avgAvailabilityRate)
                            .avgPrice(Double.isNaN(avgPrice) ? null : avgPrice)
                            .avgSafetyRating(Double.isNaN(avgSafety) ? null : avgSafety)
                            .informalChargeRate(informalChargeRate)
                            .reportCount(group.size())
                            .computedAt(now)
                            .build();
                })
                .toList();

        analyticsRepository.deleteByParkingSpotId(spotId);
        analyticsRepository.saveAll(analyticsEntries);
        log.info("Computed {} analytics entries for spot {}", analyticsEntries.size(), spotId);
    }

    @Transactional(readOnly = true)
    public SpotAnalyticsResponse getAnalytics(UUID spotId) {
        var spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new SpotNotFoundException(spotId.toString()));

        var entries = analyticsRepository.findByParkingSpotIdOrderByDayOfWeekAscHourBucketAsc(spotId);

        var dayGroups = entries.stream()
                .collect(Collectors.groupingBy(
                        SpotAnalytics::getDayOfWeek,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        var days = dayGroups.entrySet().stream()
                .map(entry -> new DayAnalytics(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(a -> new HourAnalytics(
                                        a.getHourBucket(),
                                        a.getAvgAvailabilityRate(),
                                        a.getAvgPrice(),
                                        a.getAvgSafetyRating(),
                                        a.getInformalChargeRate(),
                                        a.getReportCount()
                                ))
                                .toList()
                ))
                .toList();

        log.debug("Returning {} day groups for spot {}", days.size(), spotId);
        return new SpotAnalyticsResponse(spotId, days);
    }
}
