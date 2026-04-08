package com.relyon.parkhere.service;

import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final ParkingReportRepository reportRepository;
    private final ParkingSpotRepository spotRepository;

    static final double CONFIRMATION_WEIGHT = 0.25;
    static final double RECENCY_WEIGHT = 0.20;
    static final double DIVERSITY_WEIGHT = 0.20;
    static final double CONSISTENCY_WEIGHT = 0.20;
    static final double PROXIMITY_WEIGHT = 0.15;
    static final int TRUST_WINDOW_HOURS = 72;
    static final double RECENCY_HALF_LIFE_HOURS = 12.0;
    static final double MAX_REPORTS_FOR_SATURATION = 10.0;
    static final double MAX_USERS_FOR_SATURATION = 5.0;
    static final double MAX_PROXIMITY_METERS = 500.0;

    @Transactional
    public double recalculate(ParkingSpot spot) {
        var windowStart = LocalDateTime.now().minusHours(TRUST_WINDOW_HOURS);
        var recentReports = reportRepository.findByParkingSpotIdAndCreatedAtAfter(spot.getId(), windowStart);

        if (recentReports.isEmpty()) {
            spot.setTrustScore(0.0);
            spotRepository.save(spot);
            log.debug("Trust score for spot {} set to 0.0 — no recent reports", spot.getId());
            return 0.0;
        }

        var confirmationFactor = calculateConfirmationFactor(recentReports.size());
        var recencyFactor = calculateRecencyFactor(recentReports);
        var diversityFactor = calculateDiversityFactor(recentReports);
        var consistencyFactor = calculateConsistencyFactor(recentReports);
        var proximityFactor = calculateProximityFactor(recentReports);

        var score = (CONFIRMATION_WEIGHT * confirmationFactor)
                + (RECENCY_WEIGHT * recencyFactor)
                + (DIVERSITY_WEIGHT * diversityFactor)
                + (CONSISTENCY_WEIGHT * consistencyFactor)
                + (PROXIMITY_WEIGHT * proximityFactor);

        score = Math.min(1.0, Math.max(0.0, score));
        spot.setTrustScore(score);
        spotRepository.save(spot);

        log.debug("Trust score for spot {} recalculated: {} (conf={}, rec={}, div={}, cons={}, prox={})",
                spot.getId(), String.format("%.3f", score), String.format("%.2f", confirmationFactor),
                String.format("%.2f", recencyFactor), String.format("%.2f", diversityFactor),
                String.format("%.2f", consistencyFactor), String.format("%.2f", proximityFactor));
        return score;
    }

    static double calculateConfirmationFactor(int reportCount) {
        return Math.min(reportCount / MAX_REPORTS_FOR_SATURATION, 1.0);
    }

    static double calculateRecencyFactor(List<ParkingReport> reports) {
        var mostRecent = reports.stream()
                .map(ParkingReport::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusDays(7));

        var hoursSinceLastReport = Duration.between(mostRecent, LocalDateTime.now()).toMinutes() / 60.0;
        var lambda = Math.log(2) / RECENCY_HALF_LIFE_HOURS;
        return Math.exp(-lambda * hoursSinceLastReport);
    }

    static double calculateDiversityFactor(List<ParkingReport> reports) {
        var distinctUsers = reports.stream()
                .map(r -> r.getUser().getId())
                .distinct()
                .count();
        return Math.min(distinctUsers / MAX_USERS_FOR_SATURATION, 1.0);
    }

    static double calculateConsistencyFactor(List<ParkingReport> reports) {
        var statusCounts = reports.stream()
                .map(ParkingReport::getAvailabilityStatus)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        var dominantCount = statusCounts.values().stream()
                .max(Long::compareTo)
                .orElse(0L);

        return (double) dominantCount / reports.size();
    }

    static double calculateProximityFactor(List<ParkingReport> reports) {
        return reports.stream()
                .mapToDouble(r -> Math.max(0.0, 1.0 - (r.getGpsDistanceMeters() / MAX_PROXIMITY_METERS)))
                .average()
                .orElse(0.0);
    }
}
