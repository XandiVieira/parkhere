package com.relyon.parkhere.exception;

public class DuplicateRemovalRequestException extends DomainException {

    public DuplicateRemovalRequestException() {
        super("removal.request.duplicate");
    }
}
