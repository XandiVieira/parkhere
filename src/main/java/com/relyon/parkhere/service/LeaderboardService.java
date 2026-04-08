package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.response.LeaderboardResponse;
import com.relyon.parkhere.model.LeaderboardEntry;
import com.relyon.parkhere.model.UserPoints;
import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;
import com.relyon.parkhere.repository.LeaderboardEntryRepository;
import com.relyon.parkhere.repository.UserPointsRepository;
import com.relyon.parkhere.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardRepository;
    private final UserPointsRepository userPointsRepository;
    private final UserStreakRepository userStreakRepository;

    static final int TOP_N = 10;

    @Transactional
    public void computeWeeklyLeaderboards() {
        var periodKey = getCurrentWeekKey();
        log.info("Computing weekly leaderboards for {}", periodKey);

        computePointsLeaderboard(LeaderboardPeriod.WEEKLY, periodKey, true);
        computeStreakLeaderboard(LeaderboardPeriod.WEEKLY, periodKey);
    }

    @Transactional
    public void computeMonthlyLeaderboards() {
        var periodKey = getCurrentMonthKey();
        log.info("Computing monthly leaderboards for {}", periodKey);

        computePointsLeaderboard(LeaderboardPeriod.MONTHLY, periodKey, false);
        computeStreakLeaderboard(LeaderboardPeriod.MONTHLY, periodKey);
    }

    @Transactional
    public void resetWeeklyPoints() {
        var allPoints = userPointsRepository.findAll();
        allPoints.forEach(p -> p.setWeeklyPoints(0));
        userPointsRepository.saveAll(allPoints);
        log.info("Weekly points reset for {} users", allPoints.size());
    }

    @Transactional
    public void resetMonthlyPoints() {
        var allPoints = userPointsRepository.findAll();
        allPoints.forEach(p -> p.setMonthlyPoints(0));
        userPointsRepository.saveAll(allPoints);
        log.info("Monthly points reset for {} users", allPoints.size());
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(LeaderboardPeriod period, String periodKey, LeaderboardCategory category) {
        var resolvedKey = periodKey != null ? periodKey
                : (period == LeaderboardPeriod.WEEKLY ? getCurrentWeekKey() : getCurrentMonthKey());

        var entries = leaderboardRepository.findTop10ByPeriodAndPeriodKeyAndCategoryOrderByRankAsc(
                period, resolvedKey, category);

        var entryResponses = entries.stream()
                .map(LeaderboardResponse.LeaderboardEntryResponse::from)
                .toList();

        return new LeaderboardResponse(period, resolvedKey, category, entryResponses);
    }

    private void computePointsLeaderboard(LeaderboardPeriod period, String periodKey, boolean useWeekly) {
        var allPoints = userPointsRepository.findAll();
        var sorted = allPoints.stream()
                .sorted(Comparator.comparingInt((UserPoints p) -> useWeekly ? p.getWeeklyPoints() : p.getMonthlyPoints()).reversed())
                .limit(TOP_N)
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            var points = sorted.get(i);
            var score = useWeekly ? points.getWeeklyPoints() : points.getMonthlyPoints();
            if (score <= 0) continue;

            var entry = LeaderboardEntry.builder()
                    .user(points.getUser())
                    .period(period)
                    .periodKey(periodKey)
                    .category(LeaderboardCategory.MOST_POINTS)
                    .score(score)
                    .rank(i + 1)
                    .build();
            leaderboardRepository.save(entry);
        }
    }

    private void computeStreakLeaderboard(LeaderboardPeriod period, String periodKey) {
        var allStreaks = userStreakRepository.findAll();
        var sorted = allStreaks.stream()
                .sorted(Comparator.comparingInt(s -> -s.getCurrentStreak()))
                .limit(TOP_N)
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            var streak = sorted.get(i);
            if (streak.getCurrentStreak() <= 0) continue;

            var entry = LeaderboardEntry.builder()
                    .user(streak.getUser())
                    .period(period)
                    .periodKey(periodKey)
                    .category(LeaderboardCategory.LONGEST_STREAK)
                    .score(streak.getCurrentStreak())
                    .rank(i + 1)
                    .build();
            leaderboardRepository.save(entry);
        }
    }

    static String getCurrentWeekKey() {
        var now = LocalDate.now();
        return now.getYear() + "-W" + String.format("%02d", now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    static String getCurrentMonthKey() {
        var now = LocalDate.now();
        return now.getYear() + "-" + String.format("%02d", now.getMonthValue());
    }
}
