package com.relyon.parkhere.model;

import com.relyon.parkhere.model.enums.SpotType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "parking_spots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ParkingSpot extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotType type;

    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(nullable = false)
    @Builder.Default
    private double priceMin = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private double priceMax = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private double trustScore = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private int totalConfirmations = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
