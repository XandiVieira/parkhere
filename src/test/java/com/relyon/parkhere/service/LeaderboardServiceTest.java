package com.relyon.parkhere.service;

import com.relyon.parkhere.model.LeaderboardEntry;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.UserPoints;
import com.relyon.parkhere.model.UserStreak;
import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.LeaderboardEntryRepository;
import com.relyon.parkhere.repository.UserPointsRepository;
import com.relyon.parkhere.repository.UserStreakRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardEntryRepository leaderboardRepository;

    @Mock
    private UserPointsRepository userPointsRepository;

    @Mock
    private UserStreakRepository userStreakRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private User buildUser(String name) {
        var user = User.builder()
                .id(UUID.randomUUID()).name(name).email(name.toLowerCase() + "@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private UserPoints buildPoints(User user, int weekly, int monthly) {
        var points = UserPoints.builder()
                .id(UUID.randomUUID()).user(user)
                .totalPoints(weekly + monthly).weeklyPoints(weekly).monthlyPoints(monthly).build();
        points.setCreatedAt(LocalDateTime.now());
        points.setUpdatedAt(LocalDateTime.now());
        return points;
    }

    @Test
    void computeWeeklyLeaderboards_shouldSaveTopEntries() {
        var user1 = buildUser("Alice");
        var user2 = buildUser("Bob");
        var points1 = buildPoints(user1, 100, 200);
        var points2 = buildPoints(user2, 50, 100);
        when(userPointsRepository.findAll()).thenReturn(List.of(points1, points2));
        when(userStreakRepository.findAll()).thenReturn(List.of());
        when(leaderboardRepository.save(any(LeaderboardEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        leaderboardService.computeWeeklyLeaderboards();

        verify(leaderboardRepository, atLeast(2)).save(any(LeaderboardEntry.class));
    }

    @Test
    void resetWeeklyPoints_shouldSetAllToZero() {
        var user = buildUser("Alice");
        var points = buildPoints(user, 100, 200);
        when(userPointsRepository.findAll()).thenReturn(List.of(points));
        when(userPointsRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        leaderboardService.resetWeeklyPoints();

        assertEquals(0, points.getWeeklyPoints());
        assertEquals(200, points.getMonthlyPoints());
    }

    @Test
    void resetMonthlyPoints_shouldSetAllToZero() {
        var user = buildUser("Alice");
        var points = buildPoints(user, 100, 200);
        when(userPointsRepository.findAll()).thenReturn(List.of(points));
        when(userPointsRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        leaderboardService.resetMonthlyPoints();

        assertEquals(100, points.getWeeklyPoints());
        assertEquals(0, points.getMonthlyPoints());
    }

    @Test
    void getLeaderboard_shouldReturnEntries() {
        var user = buildUser("Alice");
        var entry = LeaderboardEntry.builder()
                .id(UUID.randomUUID()).user(user)
                .period(LeaderboardPeriod.WEEKLY).periodKey("2026-W15")
                .category(LeaderboardCategory.MOST_POINTS).score(100).rank(1).build();
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        when(leaderboardRepository.findTop10ByPeriodAndPeriodKeyAndCategoryOrderByRankAsc(
                LeaderboardPeriod.WEEKLY, "2026-W15", LeaderboardCategory.MOST_POINTS))
                .thenReturn(List.of(entry));

        var result = leaderboardService.getLeaderboard(LeaderboardPeriod.WEEKLY, "2026-W15", LeaderboardCategory.MOST_POINTS);

        assertEquals(1, result.entries().size());
        assertEquals("Alice", result.entries().getFirst().userName());
        assertEquals(100, result.entries().getFirst().score());
        assertEquals(1, result.entries().getFirst().rank());
    }

    @Test
    void getCurrentWeekKey_shouldReturnCorrectFormat() {
        var key = LeaderboardService.getCurrentWeekKey();
        assertTrue(key.matches("\\d{4}-W\\d{2}"));
    }

    @Test
    void getCurrentMonthKey_shouldReturnCorrectFormat() {
        var key = LeaderboardService.getCurrentMonthKey();
        assertTrue(key.matches("\\d{4}-\\d{2}"));
    }
}
