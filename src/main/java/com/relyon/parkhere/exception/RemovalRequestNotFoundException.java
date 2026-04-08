package com.relyon.parkhere.exception;

public class RemovalRequestNotFoundException extends DomainException {

    public RemovalRequestNotFoundException(String requestId) {
        super("removal.request.not.found", requestId);
    }
}
