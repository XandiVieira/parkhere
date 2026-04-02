package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateReportRequest;
import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotSummaryResponse;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ReportResponse submitReport(UUID spotId, CreateReportRequest request, User user) {
        var spot = findSpotOrThrow(spotId);
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
                .note(request.note())
                .gpsDistanceMeters(distance)
                .build();

        var saved = reportRepository.save(report);

        spot.setTotalConfirmations(spot.getTotalConfirmations() + 1);
        spot.setLastConfirmedAt(LocalDateTime.now());
        spotRepository.save(spot);

        log.info("Report submitted for spot {} by user {} (distance: {}m)", spotId, user.getEmail(), (int) distance);
        return ReportResponse.from(saved);
    }

    public List<ReportResponse> getReportsForSpot(UUID spotId) {
        findSpotOrThrow(spotId);
        return reportRepository.findByParkingSpotIdOrderByCreatedAtDesc(spotId).stream()
                .map(ReportResponse::from)
                .toList();
    }

    public SpotSummaryResponse getSummary(UUID spotId) {
        var spot = findSpotOrThrow(spotId);
        var recentReports = reportRepository.findByParkingSpotIdAndCreatedAtAfterOrderByCreatedAtDesc(
                spotId, LocalDateTime.now().minusHours(24)
        );

        var dominantAvailability = recentReports.stream()
                .map(ParkingReport::getAvailabilityStatus)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(AvailabilityStatus.UNKNOWN);

        var avgPrice = recentReports.stream()
                .map(ParkingReport::getEstimatedPrice)
                .filter(p -> p != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        var avgSafety = recentReports.stream()
                .map(ParkingReport::getSafetyRating)
                .filter(r -> r != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        var informalChargeCount = recentReports.stream()
                .filter(ParkingReport::isInformalChargeReported)
                .count();

        var informalPercentage = recentReports.isEmpty() ? 0.0
                : (double) informalChargeCount / recentReports.size() * 100;

        var twoHoursAgo = LocalDateTime.now().minusHours(2);
        var informalChargeRecently = recentReports.stream()
                .anyMatch(r -> r.isInformalChargeReported() && r.getCreatedAt().isAfter(twoHoursAgo));

        var lastReportAt = recentReports.stream()
                .findFirst()
                .map(ParkingReport::getCreatedAt)
                .orElse(null);

        return new SpotSummaryResponse(
                spot.getId(), spot.getName(), spot.getType(),
                spot.getLocation().getY(), spot.getLocation().getX(),
                spot.getPriceMin(), spot.getPriceMax(),
                spot.isRequiresBooking(),
                spot.getTrustScore(), spot.getTotalConfirmations(),
                spot.getLastConfirmedAt(),
                dominantAvailability,
                avgPrice > 0 ? avgPrice : null,
                avgSafety > 0 ? avgSafety : null,
                informalPercentage, informalChargeRecently, lastReportAt
        );
    }

    private ParkingSpot findSpotOrThrow(UUID spotId) {
        return spotRepository.findById(spotId)
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
