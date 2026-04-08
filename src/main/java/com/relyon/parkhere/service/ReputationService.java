package com.relyon.parkhere.service;

import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReputationService {

    private final ParkingReportRepository reportRepository;
    private final UserRepository userRepository;

    static final double MAX_REPORT_COUNT_SCORE = 40.0;
    static final double REPORTS_PER_POINT = 0.5;
    static final double MAX_PROXIMITY_SCORE = 30.0;
    static final double MAX_CONSISTENCY_SCORE = 20.0;
    static final double MAX_ACCOUNT_AGE_SCORE = 10.0;
    static final double MAX_PROXIMITY_METERS = 500.0;
    static final double DAYS_TO_MAX_AGE_BONUS = 300.0;
    static final double MAX_REPUTATION = 100.0;

    @Transactional
    public double recalculate(User user) {
        var reports = reportRepository.findByUserId(user.getId());

        if (reports.isEmpty()) {
            user.setReputationScore(0.0);
            userRepository.save(user);
            return 0.0;
        }

        var reportCountScore = calculateReportCountScore(reports.size());
        var proximityScore = calculateProximityScore(reports);
        var consistencyScore = calculateConsistencyScore(reports);
        var accountAgeScore = calculateAccountAgeScore(user);

        var score = Math.min(MAX_REPUTATION, reportCountScore + proximityScore + consistencyScore + accountAgeScore);

        user.setReputationScore(score);
        userRepository.save(user);

        log.debug("Reputation for user {} recalculated: {} (reports={}, proximity={}, consistency={}, age={})",
                user.getEmail(), String.format("%.1f", score), String.format("%.1f", reportCountScore),
                String.format("%.1f", proximityScore), String.format("%.1f", consistencyScore),
                String.format("%.1f", accountAgeScore));
        return score;
    }

    static double calculateReportCountScore(int reportCount) {
        return Math.min(reportCount * REPORTS_PER_POINT, MAX_REPORT_COUNT_SCORE);
    }

    static double calculateProximityScore(List<ParkingReport> reports) {
        var avgProximityFactor = reports.stream()
                .mapToDouble(r -> Math.max(0.0, 1.0 - (r.getGpsDistanceMeters() / MAX_PROXIMITY_METERS)))
                .average()
                .orElse(0.0);
        return avgProximityFactor * MAX_PROXIMITY_SCORE;
    }

    static double calculateConsistencyScore(List<ParkingReport> reports) {
        var reportsBySpot = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getParkingSpot().getId()));

        if (reportsBySpot.size() <= 1) {
            return MAX_CONSISTENCY_SCORE * 0.5;
        }

        var totalChecks = 0;
        var agreedChecks = 0;

        for (var entry : reportsBySpot.entrySet()) {
            var spotReports = entry.getValue();
            if (spotReports.size() < 2) continue;

            var dominant = spotReports.stream()
                    .map(ParkingReport::getAvailabilityStatus)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(AvailabilityStatus.UNKNOWN);

            for (var report : spotReports) {
                totalChecks++;
                if (report.getAvailabilityStatus() == dominant) {
                    agreedChecks++;
                }
            }
        }

        if (totalChecks == 0) return MAX_CONSISTENCY_SCORE * 0.5;
        return ((double) agreedChecks / totalChecks) * MAX_CONSISTENCY_SCORE;
    }

    static double calculateAccountAgeScore(User user) {
        var daysSinceRegistration = Duration.between(user.getCreatedAt(), LocalDateTime.now()).toDays();
        return Math.min(daysSinceRegistration / (DAYS_TO_MAX_AGE_BONUS / MAX_ACCOUNT_AGE_SCORE), MAX_ACCOUNT_AGE_SCORE);
    }
}
