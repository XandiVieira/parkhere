package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.ParkingReport;
import com.relyon.parkhere.model.enums.AvailabilityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID spotId,
        UUID userId,
        AvailabilityStatus availabilityStatus,
        Double estimatedPrice,
        Integer safetyRating,
        boolean informalChargeReported,
        String informalChargeType,
        Double informalChargeAmount,
        Integer informalChargeAggressiveness,
        String informalChargeNote,
        String note,
        double gpsDistanceMeters,
        List<ReportImageResponse> images,
        LocalDateTime createdAt
) {
    public static ReportResponse from(ParkingReport report) {
        return new ReportResponse(
                report.getId(),
                report.getParkingSpot().getId(),
                report.getUser().getId(),
                report.getAvailabilityStatus(),
                report.getEstimatedPrice(),
                report.getSafetyRating(),
                report.isInformalChargeReported(),
                report.getInformalChargeType(),
                report.getInformalChargeAmount(),
                report.getInformalChargeAggressiveness(),
                report.getInformalChargeNote(),
                report.getNote(),
                report.getGpsDistanceMeters(),
                report.getImages() != null
                        ? report.getImages().stream().map(ReportImageResponse::from).toList()
                        : List.of(),
                report.getCreatedAt()
        );
    }
}
