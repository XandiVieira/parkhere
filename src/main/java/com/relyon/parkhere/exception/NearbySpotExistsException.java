package com.relyon.parkhere.exception;

import com.relyon.parkhere.dto.response.SpotResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class NearbySpotExistsException extends DomainException {

    private final List<SpotResponse> nearbySpots;

    public NearbySpotExistsException(List<SpotResponse> nearbySpots) {
        super("spot.nearby.exists", String.valueOf(nearbySpots.size()));
        this.nearbySpots = nearbySpots;
    }
}
