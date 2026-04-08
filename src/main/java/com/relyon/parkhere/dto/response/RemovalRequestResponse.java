package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.SpotRemovalRequest;
import com.relyon.parkhere.model.enums.RemovalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RemovalRequestResponse(
        UUID id,
        UUID spotId,
        UUID requestedBy,
        String reason,
        RemovalStatus status,
        long confirmationCount,
        int confirmationsNeeded,
        LocalDateTime createdAt
) {
    public static RemovalRequestResponse from(SpotRemovalRequest request, long confirmationCount) {
        return new RemovalRequestResponse(
                request.getId(),
                request.getParkingSpot().getId(),
                request.getRequestedBy().getId(),
                request.getReason(),
                request.getStatus(),
                confirmationCount,
                request.getConfirmationsNeeded(),
                request.getCreatedAt()
        );
    }
}
