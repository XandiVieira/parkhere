package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.CreateReportRequest;
import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotSummaryResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.service.ParkingReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spots/{spotId}")
@RequiredArgsConstructor
public class ParkingReportController {

    private final ParkingReportService reportService;

    @PostMapping("/reports")
    public ResponseEntity<ReportResponse> submitReport(@PathVariable UUID spotId,
                                                       @AuthenticationPrincipal User user,
                                                       @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.submitReport(spotId, request, user));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponse>> getReports(@PathVariable UUID spotId) {
        return ResponseEntity.ok(reportService.getReportsForSpot(spotId));
    }

    @GetMapping("/summary")
    public ResponseEntity<SpotSummaryResponse> getSummary(@PathVariable UUID spotId) {
        return ResponseEntity.ok(reportService.getSummary(spotId));
    }
}
