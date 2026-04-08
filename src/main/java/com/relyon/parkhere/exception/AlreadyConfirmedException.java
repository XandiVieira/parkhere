package com.relyon.parkhere.exception;

public class AlreadyConfirmedException extends DomainException {

    public AlreadyConfirmedException() {
        super("removal.already.confirmed");
    }
}
