package com.relyon.parkhere.scheduler;

import com.relyon.parkhere.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeaderboardJobTest {

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardJob leaderboardJob;

    @Test
    void weeklyJob_shouldComputeAndReset() {
        leaderboardJob.weeklyLeaderboardAndReset();

        verify(leaderboardService).computeWeeklyLeaderboards();
        verify(leaderboardService).resetWeeklyPoints();
    }

    @Test
    void monthlyJob_shouldComputeAndReset() {
        leaderboardJob.monthlyLeaderboardAndReset();

        verify(leaderboardService).computeMonthlyLeaderboards();
        verify(leaderboardService).resetMonthlyPoints();
    }
}
