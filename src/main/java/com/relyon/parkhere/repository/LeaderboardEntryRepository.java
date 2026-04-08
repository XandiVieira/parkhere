package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.LeaderboardEntry;
import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {

    List<LeaderboardEntry> findByPeriodAndPeriodKeyAndCategoryOrderByRankAsc(
            LeaderboardPeriod period, String periodKey, LeaderboardCategory category);

    List<LeaderboardEntry> findTop10ByPeriodAndPeriodKeyAndCategoryOrderByRankAsc(
            LeaderboardPeriod period, String periodKey, LeaderboardCategory category);
}
