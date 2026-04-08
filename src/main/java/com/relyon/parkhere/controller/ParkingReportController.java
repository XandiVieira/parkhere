package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.CreateReportRequest;
import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotSummaryResponse;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.service.ParkingReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spots/{spotId}")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Reports", description = "Parking reports and spot summaries")
public class ParkingReportController {

    private final ParkingReportService reportService;

    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> submitReport(@PathVariable UUID spotId,
                                                       @AuthenticationPrincipal User user,
                                                       @Valid @RequestPart("report") CreateReportRequest request,
                                                       @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.submitReport(spotId, request, user, images));
    }

    @PostMapping(value = "/reports", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> submitReportJson(@PathVariable UUID spotId,
                                                            @AuthenticationPrincipal User user,
                                                            @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.submitReport(spotId, request, user, null));
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportResponse>> getReports(@PathVariable UUID spotId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReportsForSpot(spotId, PageRequest.of(page, size)));
    }

    @GetMapping("/summary")
    public ResponseEntity<SpotSummaryResponse> getSummary(@PathVariable UUID spotId) {
        return ResponseEntity.ok(reportService.getSummary(spotId));
    }
}
