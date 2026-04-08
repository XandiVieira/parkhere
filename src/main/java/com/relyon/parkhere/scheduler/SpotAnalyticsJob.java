package com.relyon.parkhere.scheduler;

import com.relyon.parkhere.repository.ParkingReportRepository;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.service.SpotAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("parkhere.analytics.enabled")
public class SpotAnalyticsJob {

    private final SpotAnalyticsService analyticsService;
    private final ParkingSpotRepository spotRepository;
    private final ParkingReportRepository reportRepository;

    @Scheduled(cron = "${parkhere.analytics.cron:0 0 3 * * *}")
    public void run() {
        log.info("Starting spot analytics computation job");
        var spots = spotRepository.findAll();
        var processed = 0;

        for (var spot : spots) {
            var reportCount = reportRepository.countByParkingSpotId(spot.getId());
            if (reportCount > 0) {
                analyticsService.computeAnalytics(spot);
                processed++;
            }
        }

        log.info("Spot analytics job completed — processed {} spots out of {} total", processed, spots.size());
    }
}
