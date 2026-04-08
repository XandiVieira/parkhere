package com.relyon.parkhere.service;

import com.relyon.parkhere.model.*;
import com.relyon.parkhere.model.enums.AvailabilityStatus;
import com.relyon.parkhere.model.enums.BadgeType;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GamificationServiceTest {

    @Mock
    private UserPointsRepository pointsRepository;

    @Mock
    private UserBadgeRepository badgeRepository;

    @Mock
    private UserStreakRepository streakRepository;

    @Mock
    private ParkingReportRepository reportRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private SpotRemovalConfirmationRepository confirmationRepository;

    @InjectMocks
    private GamificationService gamificationService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID()).name("Jane").email("jane@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private ParkingReport buildReport(User user, double gpsDistance) {
        var report = ParkingReport.builder()
                .id(UUID.randomUUID()).user(user)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .gpsDistanceMeters(gpsDistance).build();
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return report;
    }

    private UserPoints buildUserPoints(User user, int total, int weekly, int monthly) {
        var points = UserPoints.builder()
                .id(UUID.randomUUID()).user(user)
                .totalPoints(total).weeklyPoints(weekly).monthlyPoints(monthly).build();
        points.setCreatedAt(LocalDateTime.now());
        points.setUpdatedAt(LocalDateTime.now());
        return points;
    }

    private UserStreak buildUserStreak(User user, int current, int longest, LocalDate lastDate) {
        var streak = UserStreak.builder()
                .id(UUID.randomUUID()).user(user)
                .currentStreak(current).longestStreak(longest).lastReportDate(lastDate).build();
        streak.setCreatedAt(LocalDateTime.now());
        streak.setUpdatedAt(LocalDateTime.now());
        return streak;
    }

    @Test
    void awardPointsForReport_shouldAwardBasePoints() {
        var user = buildUser();
        var report = buildReport(user, 200.0);
        var userPoints = buildUserPoints(user, 0, 0, 0);
        var streak = buildUserStreak(user, 0, 0, null);

        when(pointsRepository.findByUserId(user.getId())).thenReturn(Optional.of(userPoints));
        when(pointsRepository.save(any(UserPoints.class))).thenAnswer(inv -> inv.getArgument(0));
        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());
        when(spotRepository.findByCreatedByIdAndActiveTrue(user.getId())).thenReturn(Collections.emptyList());
        when(confirmationRepository.countByConfirmedById(user.getId())).thenReturn(0L);
        when(badgeRepository.existsByUserIdAndBadgeType(any(), any())).thenReturn(false);

        gamificationService.awardPointsForReport(user, report);

        assertEquals(5, userPoints.getTotalPoints());
        assertEquals(5, userPoints.getWeeklyPoints());
        assertEquals(5, userPoints.getMonthlyPoints());
    }

    @Test
    void awardPointsForReport_shouldAwardGpsBonusWithin100m() {
        var user = buildUser();
        var report = buildReport(user, 50.0);
        var userPoints = buildUserPoints(user, 0, 0, 0);
        var streak = buildUserStreak(user, 0, 0, null);

        when(pointsRepository.findByUserId(user.getId())).thenReturn(Optional.of(userPoints));
        when(pointsRepository.save(any(UserPoints.class))).thenAnswer(inv -> inv.getArgument(0));
        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());
        when(spotRepository.findByCreatedByIdAndActiveTrue(user.getId())).thenReturn(Collections.emptyList());
        when(confirmationRepository.countByConfirmedById(user.getId())).thenReturn(0L);
        when(badgeRepository.existsByUserIdAndBadgeType(any(), any())).thenReturn(false);

        gamificationService.awardPointsForReport(user, report);

        assertEquals(8, userPoints.getTotalPoints());
    }

    @Test
    void awardPointsForReport_shouldNotAwardGpsBonusBeyond100m() {
        var user = buildUser();
        var report = buildReport(user, 150.0);
        var userPoints = buildUserPoints(user, 0, 0, 0);
        var streak = buildUserStreak(user, 0, 0, null);

        when(pointsRepository.findByUserId(user.getId())).thenReturn(Optional.of(userPoints));
        when(pointsRepository.save(any(UserPoints.class))).thenAnswer(inv -> inv.getArgument(0));
        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());
        when(spotRepository.findByCreatedByIdAndActiveTrue(user.getId())).thenReturn(Collections.emptyList());
        when(confirmationRepository.countByConfirmedById(user.getId())).thenReturn(0L);
        when(badgeRepository.existsByUserIdAndBadgeType(any(), any())).thenReturn(false);

        gamificationService.awardPointsForReport(user, report);

        assertEquals(5, userPoints.getTotalPoints());
    }

    @Test
    void awardPointsForReport_shouldAwardStreakBonus() {
        var user = buildUser();
        var report = buildReport(user, 200.0);
        var userPoints = buildUserPoints(user, 0, 0, 0);
        var streak = buildUserStreak(user, 3, 3, LocalDate.now().minusDays(1));

        when(pointsRepository.findByUserId(user.getId())).thenReturn(Optional.of(userPoints));
        when(pointsRepository.save(any(UserPoints.class))).thenAnswer(inv -> inv.getArgument(0));
        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());
        when(spotRepository.findByCreatedByIdAndActiveTrue(user.getId())).thenReturn(Collections.emptyList());
        when(confirmationRepository.countByConfirmedById(user.getId())).thenReturn(0L);
        when(badgeRepository.existsByUserIdAndBadgeType(any(), any())).thenReturn(false);

        gamificationService.awardPointsForReport(user, report);

        assertEquals(8, userPoints.getTotalPoints());
    }

    @Test
    void updateStreak_shouldIncrementForConsecutiveDay() {
        var user = buildUser();
        var streak = buildUserStreak(user, 3, 5, LocalDate.now().minusDays(1));

        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));

        gamificationService.updateStreak(user);

        assertEquals(4, streak.getCurrentStreak());
        assertEquals(5, streak.getLongestStreak());
        assertEquals(LocalDate.now(), streak.getLastReportDate());
    }

    @Test
    void updateStreak_shouldResetForMissedDay() {
        var user = buildUser();
        var streak = buildUserStreak(user, 5, 5, LocalDate.now().minusDays(3));

        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));

        gamificationService.updateStreak(user);

        assertEquals(1, streak.getCurrentStreak());
        assertEquals(5, streak.getLongestStreak());
        assertEquals(LocalDate.now(), streak.getLastReportDate());
    }

    @Test
    void updateStreak_shouldNotChangeForSameDay() {
        var user = buildUser();
        var streak = buildUserStreak(user, 3, 5, LocalDate.now());

        when(streakRepository.findByUserId(user.getId())).thenReturn(Optional.of(streak));
        when(streakRepository.save(any(UserStreak.class))).thenAnswer(inv -> inv.getArgument(0));

        gamificationService.updateStreak(user);

        assertEquals(3, streak.getCurrentStreak());
        assertEquals(5, streak.getLongestStreak());
        assertEquals(LocalDate.now(), streak.getLastReportDate());
    }

    @Test
    void checkAndAwardBadges_shouldAwardFirstSteps() {
        var user = buildUser();
        var report = buildReport(user, 50.0);

        when(reportRepository.findByUserId(user.getId())).thenReturn(List.of(report));
        when(spotRepository.findByCreatedByIdAndActiveTrue(user.getId())).thenReturn(Collections.emptyList());
        when(confirmationRepository.countByConfirmedById(user.getId())).thenReturn(0L);
        when(badgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.FIRST_STEPS)).thenReturn(false);
        when(badgeRepository.save(any(UserBadge.class))).thenAnswer(inv -> {
            var badge = inv.<UserBadge>getArgument(0);
            badge.setId(UUID.randomUUID());
            badge.setCreatedAt(LocalDateTime.now());
            badge.setUpdatedAt(LocalDateTime.now());
            return badge;
        });

        gamificationService.checkAndAwardBadges(user);

        verify(badgeRepository).save(argThat(badge ->
                badge.getBadgeType() == BadgeType.FIRST_STEPS && badge.getUser().equals(user)));
    }

    @Test
    void checkAndAwardBadges_shouldNotDuplicateBadge() {
        var user = buildUser();
        var report = buildReport(user, 50.0);

        when(reportRepository.findByUserId(user.getId())).thenReturn(List.of(report));
        when(spotRepository.findByCreatedByIdAndActiveTrue(user.getId())).thenReturn(Collections.emptyList());
        when(confirmationRepository.countByConfirmedById(user.getId())).thenReturn(0L);
        when(badgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.FIRST_STEPS)).thenReturn(true);

        gamificationService.checkAndAwardBadges(user);

        verify(badgeRepository, never()).save(argThat(badge ->
                badge.getBadgeType() == BadgeType.FIRST_STEPS));
    }

    @Test
    void awardPointsForPopularSpot_shouldAward10Points() {
        var user = buildUser();
        var userPoints = buildUserPoints(user, 20, 10, 15);

        when(pointsRepository.findByUserId(user.getId())).thenReturn(Optional.of(userPoints));
        when(pointsRepository.save(any(UserPoints.class))).thenAnswer(inv -> inv.getArgument(0));

        gamificationService.awardPointsForPopularSpot(user);

        assertEquals(30, userPoints.getTotalPoints());
        assertEquals(20, userPoints.getWeeklyPoints());
        assertEquals(25, userPoints.getMonthlyPoints());
    }

    @Test
    void getGamificationProfile_shouldReturnAllData() {
        var userId = UUID.randomUUID();
        var user = buildUser();
        user.setId(userId);

        var userPoints = buildUserPoints(user, 100, 30, 60);
        var badge = UserBadge.builder()
                .id(UUID.randomUUID()).user(user)
                .badgeType(BadgeType.FIRST_STEPS).earnedAt(LocalDateTime.now()).build();
        badge.setCreatedAt(LocalDateTime.now());
        badge.setUpdatedAt(LocalDateTime.now());
        var streak = buildUserStreak(user, 5, 10, LocalDate.now());

        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(userPoints));
        when(badgeRepository.findByUserIdOrderByEarnedAtDesc(userId)).thenReturn(List.of(badge));
        when(streakRepository.findByUserId(userId)).thenReturn(Optional.of(streak));

        var profile = gamificationService.getGamificationProfile(userId);

        assertEquals(100, profile.totalPoints());
        assertEquals(30, profile.weeklyPoints());
        assertEquals(60, profile.monthlyPoints());
        assertEquals(1, profile.badges().size());
        assertEquals(BadgeType.FIRST_STEPS, profile.badges().getFirst().type());
        assertEquals(5, profile.streak().currentStreak());
        assertEquals(10, profile.streak().longestStreak());
        assertEquals(LocalDate.now(), profile.streak().lastReportDate());
    }
}
