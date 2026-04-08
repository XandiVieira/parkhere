package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.response.LeaderboardResponse;
import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;
import com.relyon.parkhere.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboards")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Leaderboards", description = "Weekly and monthly leaderboards")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam LeaderboardPeriod period,
            @RequestParam LeaderboardCategory category,
            @RequestParam(required = false) String periodKey) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(period, periodKey, category));
    }
}
