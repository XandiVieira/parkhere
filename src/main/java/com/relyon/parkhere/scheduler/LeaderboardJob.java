package com.relyon.parkhere.scheduler;

import com.relyon.parkhere.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "parkhere.leaderboard.enabled", havingValue = "true", matchIfMissing = true)
public class LeaderboardJob {

    private final LeaderboardService leaderboardService;

    @Scheduled(cron = "${parkhere.leaderboard.weekly-cron:0 0 1 * * MON}")
    public void weeklyLeaderboardAndReset() {
        log.info("Weekly leaderboard job started");
        leaderboardService.computeWeeklyLeaderboards();
        leaderboardService.resetWeeklyPoints();
        log.info("Weekly leaderboard job completed");
    }

    @Scheduled(cron = "${parkhere.leaderboard.monthly-cron:0 0 2 1 * *}")
    public void monthlyLeaderboardAndReset() {
        log.info("Monthly leaderboard job started");
        leaderboardService.computeMonthlyLeaderboards();
        leaderboardService.resetMonthlyPoints();
        log.info("Monthly leaderboard job completed");
    }
}
