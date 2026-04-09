package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.response.ReportResponse;
import com.relyon.parkhere.dto.response.SpotResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.exception.ReportNotFoundException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.exception.UserNotFoundException;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingReportRepository parkingReportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SpotResponse> listSpots(Pageable pageable) {
        return parkingSpotRepository.findAll(pageable).map(SpotResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> listReports(Pageable pageable) {
        return parkingReportRepository.findAll(pageable).map(ReportResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        return Map.of(
                "totalUsers", userRepository.count(),
                "totalSpots", parkingSpotRepository.count(),
                "totalReports", parkingReportRepository.count()
        );
    }

    @Transactional
    public void deactivateSpot(UUID spotId) {
        var spot = parkingSpotRepository.findById(spotId)
                .orElseThrow(() -> new SpotNotFoundException(spotId.toString()));
        spot.setActive(false);
        parkingSpotRepository.save(spot);
        log.info("Admin deactivated spot {}", spotId);
    }

    @Transactional
    public void deleteReport(UUID reportId) {
        var report = parkingReportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId.toString()));
        parkingReportRepository.delete(report);
        log.info("Admin deleted report {}", reportId);
    }

    @Transactional
    public void banUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        user.setActive(false);
        userRepository.save(user);
        log.info("Admin banned user {}", userId);
    }

    @Transactional
    public void unbanUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        user.setActive(true);
        userRepository.save(user);
        log.info("Admin unbanned user {}", userId);
    }
}
