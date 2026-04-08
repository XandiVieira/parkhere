package com.relyon.parkhere.dto.response;

import java.util.List;
import java.util.UUID;

public record SpotAnalyticsResponse(
        UUID spotId,
        List<DayAnalytics> days
) {
    public record DayAnalytics(String dayOfWeek, List<HourAnalytics> hours) {}

    public record HourAnalytics(int hour, double availabilityRate, Double avgPrice,
                                Double avgSafetyRating, double informalChargeRate, int reportCount) {}
}
