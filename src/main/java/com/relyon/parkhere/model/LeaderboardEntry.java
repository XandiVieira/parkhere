package com.relyon.parkhere.model;

import com.relyon.parkhere.model.enums.LeaderboardCategory;
import com.relyon.parkhere.model.enums.LeaderboardPeriod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "leaderboard_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LeaderboardEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaderboardPeriod period;

    @Column(nullable = false)
    private String periodKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaderboardCategory category;

    @Column(nullable = false)
    @Builder.Default
    private int score = 0;

    private Integer rank;
}
