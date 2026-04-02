package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.ParkingSpotSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean paidOnly
) {
    public static ScheduleResponse from(ParkingSpotSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.isPaidOnly()
        );
    }
}
