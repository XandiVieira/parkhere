package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.service.AdminService;
import com.relyon.parkhere.service.LocalizedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin", description = "Admin moderation endpoints")
public class AdminController {

    private final AdminService adminService;
    private final LocalizedMessageService messageService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.listUsers(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/spots")
    public ResponseEntity<Page<SpotResponse>> listSpots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.listSpots(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportResponse>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.listReports(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @PutMapping("/spots/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateSpot(@PathVariable UUID id) {
        adminService.deactivateSpot(id);
        return ResponseEntity.ok(Map.of("message", messageService.translate("admin.spot.deactivated")));
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        adminService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/ban")
    public ResponseEntity<Map<String, String>> banUser(@PathVariable UUID id) {
        adminService.banUser(id);
        return ResponseEntity.ok(Map.of("message", messageService.translate("admin.user.banned")));
    }

    @PutMapping("/users/{id}/unban")
    public ResponseEntity<Map<String, String>> unbanUser(@PathVariable UUID id) {
        adminService.unbanUser(id);
        return ResponseEntity.ok(Map.of("message", messageService.translate("admin.user.unbanned")));
    }
}
