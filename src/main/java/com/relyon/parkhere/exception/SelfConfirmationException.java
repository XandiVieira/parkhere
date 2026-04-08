package com.relyon.parkhere.exception;

public class SelfConfirmationException extends DomainException {

    public SelfConfirmationException() {
        super("removal.self.confirmation");
    }
}
