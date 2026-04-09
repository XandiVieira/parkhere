package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateReportRequest;
import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotSummaryResponse;
import com.relyon.parkhere.exception.ReportCooldownException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.exception.TooManyImagesException;
import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.ReportImage;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.TrustLevel;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.ReportImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingReportService {

    private final ParkingReportRepository reportRepository;
    private final ParkingSpotRepository spotRepository;
    private final TrustScoreService trustScoreService;
    private final ReputationService reputationService;
    private final GamificationService gamificationService;
    private final List<ImageStorageService> imageStorageServices;
    private final ReportImageRepository reportImageRepository;

    static final int MAX_IMAGES_PER_REPORT = 3;

    static final int COOLDOWN_MINUTES = 30;

    @Transactional
    public ReportResponse submitReport(UUID spotId, CreateReportRequest request, User user, List<MultipartFile> images) {
        var spot = findSpotOrThrow(spotId);

        var cooldownStart = LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES);
        if (reportRepository.existsByParkingSpotIdAndUserIdAndCreatedAtAfter(spotId, user.getId(), cooldownStart)) {
            log.warn("Report cooldown active for user {} on spot {}", user.getEmail(), spotId);
            throw new ReportCooldownException();
        }

        var distance = calculateDistance(
                request.userLatitude(), request.userLongitude(),
                spot.getLocation().getY(), spot.getLocation().getX()
        );

        var report = ParkingReport.builder()
                .parkingSpot(spot)
                .user(user)
                .availabilityStatus(request.availabilityStatus())
                .estimatedPrice(request.estimatedPrice())
                .safetyRating(request.safetyRating())
                .informalChargeReported(request.informalChargeReported())
                .informalChargeType(request.informalChargeType())
                .informalChargeAmount(request.informalChargeAmount())
                .informalChargeAggressiveness(request.informalChargeAggressiveness())
                .informalChargeNote(request.informalChargeNote())
                .note(request.note())
                .gpsDistanceMeters(distance)
                .build();

        var saved = reportRepository.save(report);

        spot.setTotalConfirmations(spot.getTotalConfirmations() + 1);
        spot.setLastConfirmedAt(LocalDateTime.now());

        trustScoreService.recalculate(spot);
        reputationService.recalculate(user);
        gamificationService.awardPointsForReport(user, saved);

        if (spot.getTotalConfirmations() == 10) {
            gamificationService.awardPointsForPopularSpot(spot.getCreatedBy());
        }

        if (images != null && !images.isEmpty()) {
            if (images.size() > MAX_IMAGES_PER_REPORT) {
                throw new TooManyImagesException();
            }
            if (!imageStorageServices.isEmpty()) {
                var storage = imageStorageServices.getFirst();
                for (var image : images) {
                    var filename = storage.store(image);
                    var reportImage = ReportImage.builder()
                            .report(saved)
                            .filename(filename)
                            .originalFilename(image.getOriginalFilename())
                            .contentType(image.getContentType())
                            .sizeBytes(image.getSize())
                            .build();
                    reportImageRepository.save(reportImage);
                    saved.getImages().add(reportImage);
                }
            }
        }

        log.info("Report submitted for spot {} by user {} (distance: {}m)", spotId, user.getEmail(), (int) distance);
        return ReportResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsForSpot(UUID spotId, Pageable pageable) {
        findSpotOrThrow(spotId);
        return reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spotId, pageable)
                .map(ReportResponse::from);
    }

    @Transactional(readOnly = true)
    public SpotSummaryResponse getSummary(UUID spotId) {
        var spot = findSpotOrThrow(spotId);
        var recentReports = reportRepository.findByParkingSpotIdAndCreatedAtAfterOrderByCreatedAtDesc(
                spotId, LocalDateTime.now().minusHours(24)
        );

        var dominantAvailability = calculateWeightedDominantAvailability(recentReports);

        var avgPrice = calculateWeightedAveragePrice(recentReports);

        var avgSafety = calculateWeightedAverageSafety(recentReports);

        var informalChargeCount = recentReports.stream()
                .filter(ParkingReport::isInformalChargeReported)
                .count();

        var informalPercentage = recentReports.isEmpty() ? 0.0
                : (double) informalChargeCount / recentReports.size() * 100;

        var twoHoursAgo = LocalDateTime.now().minusHours(2);
        var informalChargeRecently = recentReports.stream()
                .anyMatch(r -> r.isInformalChargeReported() && r.getCreatedAt().isAfter(twoHoursAgo));

        var availableCount = recentReports.stream()
                .filter(r -> r.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .count();
        var availabilityRate = recentReports.isEmpty() ? 0.0
                : (double) availableCount / recentReports.size();

        var lastReportAt = recentReports.stream()
                .findFirst()
                .map(ParkingReport::getCreatedAt)
                .orElse(null);

        return new SpotSummaryResponse(
                spot.getId(), spot.getName(), spot.getType(),
                spot.getLocation().getY(), spot.getLocation().getX(),
                spot.getPriceMin(), spot.getPriceMax(),
                spot.isRequiresBooking(),
                spot.getAddress(),
                spot.getTrustScore(), TrustLevel.fromScore(spot.getTrustScore()),
                spot.getTotalConfirmations(),
                spot.getLastConfirmedAt(),
                dominantAvailability,
                avgPrice,
                avgSafety,
                informalPercentage, informalChargeRecently, availabilityRate, lastReportAt
        );
    }

    static double getReputationWeight(ParkingReport report) {
        return 1.0 + (report.getUser().getReputationScore() / 100.0);
    }

    private AvailabilityStatus calculateWeightedDominantAvailability(List<ParkingReport> reports) {
        var weightedCounts = new java.util.HashMap<AvailabilityStatus, Double>();
        for (var report : reports) {
            var weight = getReputationWeight(report);
            weightedCounts.merge(report.getAvailabilityStatus(), weight, Double::sum);
        }
        return weightedCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(AvailabilityStatus.UNKNOWN);
    }

    private Double calculateWeightedAveragePrice(List<ParkingReport> reports) {
        var totalWeight = 0.0;
        var weightedSum = 0.0;
        for (var report : reports) {
            if (report.getEstimatedPrice() != null) {
                var weight = getReputationWeight(report);
                weightedSum += report.getEstimatedPrice() * weight;
                totalWeight += weight;
            }
        }
        return totalWeight > 0 ? weightedSum / totalWeight : null;
    }

    private Double calculateWeightedAverageSafety(List<ParkingReport> reports) {
        var totalWeight = 0.0;
        var weightedSum = 0.0;
        for (var report : reports) {
            if (report.getSafetyRating() != null) {
                var weight = getReputationWeight(report);
                weightedSum += report.getSafetyRating() * weight;
                totalWeight += weight;
            }
        }
        return totalWeight > 0 ? weightedSum / totalWeight : null;
    }

    private ParkingSpot findSpotOrThrow(UUID spotId) {
        return spotRepository.findByIdAndActiveTrue(spotId)
                .orElseThrow(() -> new SpotNotFoundException(spotId.toString()));
    }

    static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        var earthRadius = 6_371_000.0;
        var dLat = Math.toRadians(lat2 - lat1);
        var dLon = Math.toRadians(lon2 - lon1);
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
