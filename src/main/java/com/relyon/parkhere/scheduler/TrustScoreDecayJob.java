package com.relyon.parkhere.scheduler;

import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "parkhere.decay.enabled", havingValue = "true", matchIfMissing = true)
public class TrustScoreDecayJob {

    private final ParkingSpotRepository spotRepository;
    private final TrustScoreService trustScoreService;

    @Scheduled(cron = "${parkhere.decay.cron:0 0 * * * *}")
    public void decayTrustScores() {
        var spots = spotRepository.findByActiveTrueAndTrustScoreGreaterThan(0.0);
        log.info("Trust score decay job started — processing {} spots", spots.size());

        var updated = 0;
        for (var spot : spots) {
            var oldScore = spot.getTrustScore();
            var newScore = trustScoreService.recalculate(spot);
            if (Math.abs(oldScore - newScore) > 0.001) {
                updated++;
            }
        }

        log.info("Trust score decay job completed — {} scores updated", updated);
    }
}
