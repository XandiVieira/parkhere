package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.response.GamificationResponse;
import com.relyon.parkhere.dto.response.GamificationResponse.BadgeResponse;
import com.relyon.parkhere.dto.response.GamificationResponse.StreakResponse;
import com.relyon.parkhere.model.*;
import com.relyon.parkhere.model.enums.BadgeType;
import com.relyon.parkhere.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private static final int BASE_REPORT_POINTS = 5;
    private static final int GPS_BONUS_POINTS = 3;
    private static final double GPS_BONUS_MAX_DISTANCE = 100.0;
    private static final int MAX_STREAK_BONUS = 7;
    private static final int POPULAR_SPOT_POINTS = 10;

    private final UserPointsRepository pointsRepository;
    private final UserBadgeRepository badgeRepository;
    private final UserStreakRepository streakRepository;
    private final ParkingReportRepository reportRepository;
    private final ParkingSpotRepository spotRepository;
    private final SpotRemovalConfirmationRepository confirmationRepository;

    @Transactional
    public void awardPointsForReport(User user, ParkingReport report) {
        var points = BASE_REPORT_POINTS;

        if (report.getGpsDistanceMeters() <= GPS_BONUS_MAX_DISTANCE) {
            points += GPS_BONUS_POINTS;
            log.debug("GPS bonus awarded for user {} (distance: {}m)", user.getId(), report.getGpsDistanceMeters());
        }

        var streak = getOrCreateStreak(user);
        var streakBonus = Math.min(streak.getCurrentStreak(), MAX_STREAK_BONUS);
        points += streakBonus;

        var userPoints = getOrCreatePoints(user);
        userPoints.setTotalPoints(userPoints.getTotalPoints() + points);
        userPoints.setWeeklyPoints(userPoints.getWeeklyPoints() + points);
        userPoints.setMonthlyPoints(userPoints.getMonthlyPoints() + points);
        pointsRepository.save(userPoints);

        log.info("Awarded {} points to user {} for report (base={}, gps={}, streak={})",
                points, user.getId(), BASE_REPORT_POINTS,
                report.getGpsDistanceMeters() <= GPS_BONUS_MAX_DISTANCE ? GPS_BONUS_POINTS : 0,
                streakBonus);

        updateStreak(user);
        checkAndAwardBadges(user);
    }

    @Transactional
    public void awardPointsForSpotCreation(User user) {
        log.info("Checking badges after spot creation by user {}", user.getId());
        checkAndAwardBadges(user);
    }

    @Transactional
    public void awardPointsForPopularSpot(User user) {
        var userPoints = getOrCreatePoints(user);
        userPoints.setTotalPoints(userPoints.getTotalPoints() + POPULAR_SPOT_POINTS);
        userPoints.setWeeklyPoints(userPoints.getWeeklyPoints() + POPULAR_SPOT_POINTS);
        userPoints.setMonthlyPoints(userPoints.getMonthlyPoints() + POPULAR_SPOT_POINTS);
        pointsRepository.save(userPoints);

        log.info("Awarded {} popular spot points to user {}", POPULAR_SPOT_POINTS, user.getId());
    }

    @Transactional
    public void checkAndAwardBadges(User user) {
        var userId = user.getId();
        var reports = reportRepository.findByUserId(userId);
        var reportCount = reports.size();
        var spotCount = spotRepository.findByCreatedByIdAndActiveTrue(userId).size();
        var confirmationCount = confirmationRepository.countByConfirmedById(userId);

        checkReportMilestoneBadges(user, reportCount);
        checkSpotCreationBadges(user, spotCount);
        checkTimeBadges(user, reports);
        checkCommunityBadge(user, confirmationCount);
    }

    @Transactional
    public void updateStreak(User user) {
        var streak = getOrCreateStreak(user);
        var today = LocalDate.now();
        var lastDate = streak.getLastReportDate();

        if (lastDate == null || lastDate.isBefore(today.minusDays(1))) {
            streak.setCurrentStreak(1);
        } else if (lastDate.equals(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        }
        // if lastDate == today, no change

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastReportDate(today);
        streakRepository.save(streak);

        log.debug("Streak updated for user {}: current={}, longest={}",
                user.getId(), streak.getCurrentStreak(), streak.getLongestStreak());
    }

    @Transactional(readOnly = true)
    public GamificationResponse getGamificationProfile(UUID userId) {
        var userPoints = pointsRepository.findByUserId(userId)
                .orElse(UserPoints.builder().totalPoints(0).weeklyPoints(0).monthlyPoints(0).build());

        var badges = badgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .map(BadgeResponse::from)
                .toList();

        var streak = streakRepository.findByUserId(userId)
                .map(StreakResponse::from)
                .orElse(new StreakResponse(0, 0, null));

        log.debug("Loaded gamification profile for user {}: {} points, {} badges",
                userId, userPoints.getTotalPoints(), badges.size());

        return new GamificationResponse(
                userPoints.getTotalPoints(),
                userPoints.getWeeklyPoints(),
                userPoints.getMonthlyPoints(),
                badges,
                streak
        );
    }

    private void checkReportMilestoneBadges(User user, int reportCount) {
        if (reportCount >= 1) {
            awardBadgeIfNew(user, BadgeType.FIRST_STEPS, 0);
        }
        if (reportCount >= 10) {
            awardBadgeIfNew(user, BadgeType.REGULAR, 5);
        }
        if (reportCount >= 50) {
            awardBadgeIfNew(user, BadgeType.VETERAN, 20);
        }
        if (reportCount >= 100) {
            awardBadgeIfNew(user, BadgeType.CENTURION, 50);
        }
    }

    private void checkSpotCreationBadges(User user, int spotCount) {
        if (spotCount >= 1) {
            awardBadgeIfNew(user, BadgeType.SPOT_DISCOVERER, 0);
        }
        if (spotCount >= 10) {
            awardBadgeIfNew(user, BadgeType.CARTOGRAPHER, 10);
        }
    }

    private void checkTimeBadges(User user, List<ParkingReport> reports) {
        var nightReports = reports.stream()
                .filter(r -> {
                    var hour = r.getCreatedAt().getHour();
                    return hour >= 22 || hour < 6;
                })
                .count();

        var earlyReports = reports.stream()
                .filter(r -> {
                    var hour = r.getCreatedAt().getHour();
                    return hour >= 6 && hour < 9;
                })
                .count();

        if (nightReports >= 10) {
            awardBadgeIfNew(user, BadgeType.NIGHT_OWL, 5);
        }
        if (earlyReports >= 10) {
            awardBadgeIfNew(user, BadgeType.EARLY_BIRD, 5);
        }
    }

    private void checkCommunityBadge(User user, long confirmationCount) {
        if (confirmationCount >= 5) {
            awardBadgeIfNew(user, BadgeType.COMMUNITY_GUARDIAN, 10);
        }
    }

    private void awardBadgeIfNew(User user, BadgeType badgeType, int bonusPoints) {
        if (badgeRepository.existsByUserIdAndBadgeType(user.getId(), badgeType)) {
            return;
        }

        var badge = UserBadge.builder()
                .user(user)
                .badgeType(badgeType)
                .earnedAt(LocalDateTime.now())
                .build();
        badgeRepository.save(badge);

        if (bonusPoints > 0) {
            var userPoints = getOrCreatePoints(user);
            userPoints.setTotalPoints(userPoints.getTotalPoints() + bonusPoints);
            userPoints.setWeeklyPoints(userPoints.getWeeklyPoints() + bonusPoints);
            userPoints.setMonthlyPoints(userPoints.getMonthlyPoints() + bonusPoints);
            pointsRepository.save(userPoints);
        }

        log.info("Badge {} awarded to user {} (bonus: {} points)", badgeType, user.getId(), bonusPoints);
    }

    private UserPoints getOrCreatePoints(User user) {
        return pointsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    var newPoints = UserPoints.builder().user(user).build();
                    return pointsRepository.save(newPoints);
                });
    }

    private UserStreak getOrCreateStreak(User user) {
        return streakRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    var newStreak = UserStreak.builder().user(user).build();
                    return streakRepository.save(newStreak);
                });
    }
}
