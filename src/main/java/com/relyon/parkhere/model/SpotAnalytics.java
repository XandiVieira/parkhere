package com.relyon.parkhere.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "spot_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SpotAnalytics extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_spot_id", nullable = false)
    private ParkingSpot parkingSpot;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false)
    private int hourBucket;

    @Column(nullable = false)
    @Builder.Default
    private double avgAvailabilityRate = 0.0;

    private Double avgPrice;

    private Double avgSafetyRating;

    @Column(nullable = false)
    @Builder.Default
    private double informalChargeRate = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private int reportCount = 0;

    @Column(nullable = false)
    private LocalDateTime computedAt;
}
