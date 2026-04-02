package com.relyon.parkhere.exception;

public class SpotNotFoundException extends DomainException {

    public SpotNotFoundException(String spotId) {
        super("spot.not.found", spotId);
    }
}
