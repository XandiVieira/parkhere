package com.relyon.parkhere.model;

import com.relyon.parkhere.model.enums.RemovalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "spot_removal_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SpotRemovalRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_spot_id", nullable = false)
    private ParkingSpot parkingSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RemovalStatus status = RemovalStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int confirmationsNeeded = 3;

    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "removalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SpotRemovalConfirmation> confirmations = new ArrayList<>();
}
