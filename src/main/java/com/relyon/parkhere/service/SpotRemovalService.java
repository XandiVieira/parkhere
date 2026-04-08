package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.CreateRemovalRequest;
import com.relyon.parkhere.dto.response.RemovalRequestResponse;
import com.relyon.parkhere.exception.*;
import com.relyon.parkhere.model.SpotRemovalConfirmation;
import com.relyon.parkhere.model.SpotRemovalRequest;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.RemovalStatus;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.SpotRemovalConfirmationRepository;
import com.relyon.parkhere.repository.SpotRemovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotRemovalService {

    private final SpotRemovalRequestRepository removalRequestRepository;
    private final SpotRemovalConfirmationRepository confirmationRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    @Transactional
    public RemovalRequestResponse requestRemoval(UUID spotId, CreateRemovalRequest request, User user) {
        var spot = parkingSpotRepository.findByIdAndActiveTrue(spotId)
                .orElseThrow(() -> new SpotNotFoundException(spotId.toString()));

        removalRequestRepository.findByParkingSpotIdAndStatusAndRequestedById(spotId, RemovalStatus.PENDING, user.getId())
                .ifPresent(existing -> {
                    throw new DuplicateRemovalRequestException();
                });

        var removalRequest = SpotRemovalRequest.builder()
                .parkingSpot(spot)
                .requestedBy(user)
                .reason(request.reason())
                .status(RemovalStatus.PENDING)
                .build();

        var saved = removalRequestRepository.save(removalRequest);
        log.info("Removal request {} created for spot {} by user {}", saved.getId(), spotId, user.getEmail());
        return RemovalRequestResponse.from(saved, 0);
    }

    @Transactional
    public RemovalRequestResponse confirmRemoval(UUID requestId, User user) {
        var removalRequest = removalRequestRepository.findById(requestId)
                .filter(r -> r.getStatus() == RemovalStatus.PENDING)
                .orElseThrow(() -> new RemovalRequestNotFoundException(requestId.toString()));

        if (removalRequest.getRequestedBy().getId().equals(user.getId())) {
            throw new SelfConfirmationException();
        }

        if (confirmationRepository.existsByRemovalRequestIdAndConfirmedById(requestId, user.getId())) {
            throw new AlreadyConfirmedException();
        }

        var confirmation = SpotRemovalConfirmation.builder()
                .removalRequest(removalRequest)
                .confirmedBy(user)
                .build();
        confirmationRepository.save(confirmation);

        var count = confirmationRepository.countByRemovalRequestId(requestId);
        log.info("Removal request {} confirmed by user {} ({}/{})", requestId, user.getEmail(), count, removalRequest.getConfirmationsNeeded());

        if (count >= removalRequest.getConfirmationsNeeded()) {
            removalRequest.setStatus(RemovalStatus.CONFIRMED);
            removalRequest.setResolvedAt(LocalDateTime.now());
            removalRequestRepository.save(removalRequest);

            var spot = removalRequest.getParkingSpot();
            spot.setActive(false);
            parkingSpotRepository.save(spot);
            log.info("Spot {} deactivated after {} confirmations on removal request {}", spot.getId(), count, requestId);
        }

        return RemovalRequestResponse.from(removalRequest, count);
    }

    @Transactional(readOnly = true)
    public List<RemovalRequestResponse> getPendingRemovals(UUID spotId) {
        parkingSpotRepository.findByIdAndActiveTrue(spotId)
                .orElseThrow(() -> new SpotNotFoundException(spotId.toString()));

        return removalRequestRepository.findByParkingSpotIdAndStatus(spotId, RemovalStatus.PENDING).stream()
                .map(r -> RemovalRequestResponse.from(r, confirmationRepository.countByRemovalRequestId(r.getId())))
                .toList();
    }
}
