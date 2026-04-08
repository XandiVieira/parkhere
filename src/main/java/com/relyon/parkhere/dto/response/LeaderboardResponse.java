package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.LeaderboardEntry;
import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;

import java.util.List;

public record LeaderboardResponse(
        LeaderboardPeriod period,
        String periodKey,
        LeaderboardCategory category,
        List<LeaderboardEntryResponse> entries
) {
    public record LeaderboardEntryResponse(
            int rank,
            String userName,
            int score
    ) {
        public static LeaderboardEntryResponse from(LeaderboardEntry entry) {
            return new LeaderboardEntryResponse(
                    entry.getRank(),
                    entry.getUser().getName(),
                    entry.getScore()
            );
        }
    }
}
