package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.UserBadge;
import com.relyon.parkhere.model.UserStreak;
import com.relyon.parkhere.model.enums.BadgeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GamificationResponse(
        int totalPoints,
        int weeklyPoints,
        int monthlyPoints,
        List<BadgeResponse> badges,
        StreakResponse streak
) {
    public record BadgeResponse(BadgeType type, LocalDateTime earnedAt) {
        public static BadgeResponse from(UserBadge badge) {
            return new BadgeResponse(badge.getBadgeType(), badge.getEarnedAt());
        }
    }

    public record StreakResponse(int currentStreak, int longestStreak, LocalDate lastReportDate) {
        public static StreakResponse from(UserStreak streak) {
            return new StreakResponse(streak.getCurrentStreak(), streak.getLongestStreak(), streak.getLastReportDate());
        }
    }
}
