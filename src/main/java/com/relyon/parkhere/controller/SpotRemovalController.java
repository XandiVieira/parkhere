package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.CreateRemovalRequest;
import com.relyon.parkhere.dto.response.RemovalRequestResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.service.SpotRemovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spots/{spotId}/removal-requests")
@RequiredArgsConstructor
public class SpotRemovalController {

    private final SpotRemovalService spotRemovalService;

    @PostMapping
    public ResponseEntity<RemovalRequestResponse> requestRemoval(@PathVariable UUID spotId,
                                                                  @AuthenticationPrincipal User user,
                                                                  @Valid @RequestBody CreateRemovalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotRemovalService.requestRemoval(spotId, request, user));
    }

    @PostMapping("/{requestId}/confirm")
    public ResponseEntity<RemovalRequestResponse> confirmRemoval(@PathVariable UUID spotId,
                                                                  @PathVariable UUID requestId,
                                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(spotRemovalService.confirmRemoval(requestId, user));
    }

    @GetMapping
    public ResponseEntity<List<RemovalRequestResponse>> getPendingRemovals(@PathVariable UUID spotId) {
        return ResponseEntity.ok(spotRemovalService.getPendingRemovals(spotId));
    }
}
