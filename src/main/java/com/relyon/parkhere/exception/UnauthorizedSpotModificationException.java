package com.relyon.parkhere.exception;

public class UnauthorizedSpotModificationException extends DomainException {

    public UnauthorizedSpotModificationException() {
        super("spot.update.unauthorized");
    }
}
