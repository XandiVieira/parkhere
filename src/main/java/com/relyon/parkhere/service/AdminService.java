package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.ReportNotFoundException;
import com.relyon.parkhere.exception.SpotNotFoundException;
import com.relyon.parkhere.exception.UserNotFoundException;
import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingReportRepository parkingReportRepository;
    private final UserRepository userRepository;

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
